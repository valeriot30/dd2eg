package com.dd2eg.repository;

import com.dd2eg.DTO.*;
import com.dd2eg.backend.neo4j.dto.*;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Repository per l'esecuzione delle query Cypher sul Graph DB Neo4j.
 * Utilizza il Neo4j Java Driver diretto (non Spring Data Neo4j)
 * per avere pieno controllo su transazioni e parametri.
 *
 * Tutte le query usano session.executeRead() per garantire
 * che vengano eseguite su transazioni di sola lettura, ottimizzando
 * il routing verso i follower/replica in un cluster Neo4j.
 *
 * I parametri sono sempre passati via Map.of() per prevenire Cypher injection.
 */
@Repository
public class Neo4jRecommendationRepository {

    private final Driver driver;

    // QUERY CYPHER

    /**
     * Query 1 — Project Recommendation per Developer.
     * Trova progetti aperti che condividono tag con quelli su cui il dev ha
     * lavorato, e che hanno task aperti compatibili con le sue skill.
     */
    private static final String PROJECT_RECOMMENDATION_QUERY = """
            MATCH (dev:Developer {id: $devId})-[:WORK_ON]->(:Task)-[:BELONGS_TO]->(:Project)-[:CATEGORIZED_BY]->(tag:Tag)
            MATCH (tag)<-[:CATEGORIZED_BY]-(recProj:Project {status:'open'})<-[:BELONGS_TO]-(task:Task {status: 'open'})
            WHERE NOT (dev)-[:WORK_ON]->(task) AND NOT (dev)-[:CREATED]->(recProj)
            MATCH (dev)-[:HAS_SKILL]->(skill:Skill)<-[:REQUIRES_SKILL]-(task)
            RETURN recProj.id AS RecommendedProject,
                   count(DISTINCT tag) AS SharedTagCount,
                   count(DISTINCT task) AS OpenMatchingTasks,
                   collect(DISTINCT skill.name) AS MatchingSkills
            ORDER BY SharedTagCount DESC, OpenMatchingTasks DESC
            LIMIT 10
            """;

    /**
     * Query 2 — Skills Recommendation.
     * Analizza i task aperti correlati alle skill già possedute dal dev
     * e suggerisce le skill più richieste che il dev non ha ancora.
     */
    private static final String SKILL_RECOMMENDATION_QUERY = """
            MATCH (dev:Developer {id: $devId})-[:HAS_SKILL]->(knownSkill:Skill)
            CALL (knownSkill) {
              MATCH (knownSkill)<-[:REQUIRES_SKILL]-(t:Task {status: 'open'})
              ORDER BY t.created_at DESC
              RETURN t LIMIT 100
            }
            MATCH (t)-[:REQUIRES_SKILL]->(recommended:Skill)
            WHERE NOT (dev)-[:HAS_SKILL]->(recommended)
            RETURN recommended.name AS RecommendedSkill,
                   count(DISTINCT t) AS Frequency
            ORDER BY Frequency DESC
            LIMIT 5
            """;

    /**
     * Query 3 — Task Ranking per Developer in un Progetto.
     * Ordina i task aperti di un progetto in base al match
     * tra le skill richieste dal task e quelle possedute dal dev.
     */
    private static final String TASK_RANKING_QUERY = """
            MATCH (proj:Project {id: $projId})<-[:BELONGS_TO]-(task:Task {status: 'open'})
            MATCH (dev:Developer {id: $devId})
            OPTIONAL MATCH (task)-[:REQUIRES_SKILL]->(reqSkill:Skill)<-[:HAS_SKILL]-(dev)
            RETURN task.id AS Task,
                   task.priority AS Priority,
                   collect(reqSkill.name) AS MatchedSkills,
                   count(reqSkill) AS MatchScore
            ORDER BY MatchScore DESC, Priority DESC
            """;

    /**
     * Query 4 — Financing Recommendation per Enterprise.
     * Suggerisce progetti aperti basandosi sui tag dei progetti
     * precedentemente finanziati dall'Enterprise.
     */
    private static final String FINANCING_RECOMMENDATION_QUERY = """
            MATCH (ent:Enterprise {id: $entId})-[:FINANCED]->(:Task)-[:BELONGS_TO]->(:Project)-[:CATEGORIZED_BY]->(tag:Tag)
            MATCH (tag)<-[:CATEGORIZED_BY]-(recProj:Project {status:'open'})
            WHERE NOT (ent)-[:FINANCED]->(:Task)-[:BELONGS_TO]-(recProj)
            OPTIONAL MATCH (recProj)<-[:BELONGS_TO]-(openTask:Task {status: 'open'})
            RETURN recProj.id AS RecommendedProject,
                   count(DISTINCT tag) AS ShareTagCount,
                   collect(DISTINCT tag.name) AS MatchingTags,
                   count(DISTINCT openTask) AS AvailableTask
            ORDER BY ShareTagCount DESC, AvailableTask DESC
            LIMIT 10
            """;

    //TODO this is wrong
    /**
     * Query 5 — Anomaly Detection Batch (Fraud/Escrow Loop Prevention).
     * Identifica cicli sospetti: Enterprise finanzia task → Dev ci lavora →
     * Dev ha creato un progetto → quel progetto ha task finanziati dalla stessa
     * Enterprise.
     */
    private static final String ANOMALY_DETECTION_BATCH_QUERY = """
            MATCH path = ((ent:Enterprise)-[:FINANCED]->(t1:Task)<-[:WORK_ON]-(dev:Developer)-[:CREATED]->(proj:Project)<-[:BELONGS_TO]-(t2:Task)<-[:FINANCED]-(ent))
            WHERE t1 <> t2
            RETURN ent.id AS EnterpriseId,
                   dev.id AS SuspiciousDeveloper,
                   count(path) AS CycleFrequency,
                   collect(DISTINCT t1.id) AS TasksWorked,
                   collect(DISTINCT t2.id) AS FinancedTasksInTheirProject
            ORDER BY CycleFrequency DESC
            """;


    //TODO THIS IS WRONG
    /**
     * Query 5 (single enterprise) — Anomaly detection filtered by a specific Enterprise.
     * Triggered after a FUNDING event is synced to Neo4j.
     */
    private static final String ANOMALY_DETECTION_SINGLE_QUERY = """
            MATCH path = ((ent:Enterprise {id: $entId})-[:FINANCED]->(t1:Task)<-[:WORK_ON]-(dev:Developer)-[:CREATED]->(proj:Project)<-[:BELONGS_TO]-(t2:Task)<-[:FINANCED]-(ent))
            WHERE t1 <> t2
            RETURN ent.id AS EnterpriseId,
                   dev.id AS SuspiciousDeveloper,
                   count(path) AS CycleFrequency,
                   collect(DISTINCT t1.id) AS TasksWorked,
                   collect(DISTINCT t2.id) AS FinancedTasksInTheirProject
            ORDER BY CycleFrequency DESC
            """;

    public Neo4jRecommendationRepository(Driver driver) {
        this.driver = driver;
    }

    // QUERY 1 — Project Recommendation per Developer
    public List<ProjectRecommendationDTO> getProjectRecommendations(String devId) {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            return session.executeRead(tx -> {
                Result result = tx.run(PROJECT_RECOMMENDATION_QUERY, Map.of("devId", devId));

                List<ProjectRecommendationDTO> recommendations = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    recommendations.add(new ProjectRecommendationDTO(
                            record.get("RecommendedProject").asString(),
                            record.get("SharedTagCount").asLong(),
                            record.get("OpenMatchingTasks").asLong(),
                            record.get("MatchingSkills").asList(Value::asString)));
                }
                return recommendations;
            });
        }
    }

    // QUERY 2 — Skills Recommendation
    public List<SkillRecommendationDTO> getSkillRecommendations(String devId) {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            return session.executeRead(tx -> {
                Result result = tx.run(SKILL_RECOMMENDATION_QUERY, Map.of("devId", devId));

                List<SkillRecommendationDTO> recommendations = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    recommendations.add(new SkillRecommendationDTO(
                            record.get("RecommendedSkill").asString(),
                            record.get("Frequency").asLong()));
                }
                return recommendations;
            });
        }
    }

    // QUERY 3 — Task Ranking
    public List<TaskRankingDTO> getTaskRanking(String projId, String devId) {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            return session.executeRead(tx -> {
                Result result = tx.run(TASK_RANKING_QUERY, Map.of("projId", projId, "devId", devId));

                List<TaskRankingDTO> rankings = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    rankings.add(new TaskRankingDTO(
                            record.get("Task").asString(),
                            record.get("Priority").asLong(),
                            record.get("MatchedSkills").asList(Value::asString),
                            record.get("MatchScore").asLong()));
                }
                return rankings;
            });
        }
    }

    // QUERY 4 — Financing Recommendation per Enterprise
    public List<FinancingRecommendationDTO> getFinancingRecommendations(String entId) {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            return session.executeRead(tx -> {
                Result result = tx.run(FINANCING_RECOMMENDATION_QUERY, Map.of("entId", entId));

                List<FinancingRecommendationDTO> recommendations = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    recommendations.add(new FinancingRecommendationDTO(
                            record.get("RecommendedProject").asString(),
                            record.get("ShareTagCount").asLong(),
                            record.get("MatchingTags").asList(Value::asString),
                            record.get("AvailableTask").asLong()));
                }
                return recommendations;
            });
        }
    }

    // QUERY 5 — Anomaly Detection Batch (Fraud Prevention)
    public List<AnomalyDetectionDTO> detectAnomalies() {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            return session.executeRead(tx -> {
                Result result = tx.run(ANOMALY_DETECTION_BATCH_QUERY);

                List<AnomalyDetectionDTO> anomalies = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    anomalies.add(new AnomalyDetectionDTO(
                            record.get("EnterpriseId").asString(),
                            record.get("SuspiciousDeveloper").asString(),
                            record.get("CycleFrequency").asLong(),
                            record.get("TasksWorked").asList(Value::asString),
                            record.get("FinancedTasksInTheirProject").asList(Value::asString)));
                }
                return anomalies;
            });
        }
    }

    // QUERY 5 (single) — Anomaly Detection for a specific Enterprise
    public List<AnomalyDetectionDTO> detectAnomaliesForEnterprise(String enterpriseId) {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            return session.executeRead(tx -> {
                Result result = tx.run(ANOMALY_DETECTION_SINGLE_QUERY, Map.of("entId", enterpriseId));

                List<AnomalyDetectionDTO> anomalies = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    anomalies.add(new AnomalyDetectionDTO(
                            record.get("EnterpriseId").asString(),
                            record.get("SuspiciousDeveloper").asString(),
                            record.get("CycleFrequency").asLong(),
                            record.get("TasksWorked").asList(Value::asString),
                            record.get("FinancedTasksInTheirProject").asList(Value::asString)));
                }
                return anomalies;
            });
        }
    }
}
