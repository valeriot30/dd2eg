package com.dd2eg.backend.repository;

import com.dd2eg.backend.DTO.*;
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

    // --- NUOVE QUERY ANOMALY DETECTION ---

    private static final String ANOMALY_DETECTION_CROSS_ENTERPRISE_QUERY = """
            MATCH (e1:Enterprise)-[:FINANCED]->(t1:Task)-[:BELONGS_TO]->(p1:Project)<-[:CREATED]-(e2:Enterprise)
            MATCH (e2)-[:FINANCED]->(t2:Task)-[:BELONGS_TO]->(p2:Project)<-[:CREATED]-(e1)
            WHERE e1.id < e2.id
            RETURN e1.id AS EnterpriseA,
                   count(DISTINCT t1) AS TasksFinancedByA_in_B,
                   e2.id AS EnterpriseB,
                   count(DISTINCT t2) AS TasksFinancedByB_in_A
            ORDER BY TasksFinancedByA_in_B + TasksFinancedByB_in_A DESC;
            """;

    private static final String ANOMALY_DETECTION_CROSS_ENTERPRISE_SINGLE_QUERY = """
            MATCH (e1:Enterprise {id: $entId})-[:FINANCED]->(t1:Task)-[:BELONGS_TO]->(p1:Project)<-[:CREATED]-(e2:Enterprise)
            MATCH (e2)-[:FINANCED]->(t2:Task)-[:BELONGS_TO]->(p2:Project)<-[:CREATED]-(e1)
            WHERE e1 <> e2
            RETURN e1.id AS EnterpriseA,
                   count(DISTINCT t1) AS TasksFinancedByA_in_B,
                   e2.id AS EnterpriseB,
                   count(DISTINCT t2) AS TasksFinancedByB_in_A
            ORDER BY TasksFinancedByA_in_B + TasksFinancedByB_in_A DESC;
            """;

    private static final String ANOMALY_DETECTION_DEVELOPER_ENTERPRISE_QUERY = """
            MATCH (ent:Enterprise)-[:FINANCED]->(t:Task)<-[:WORK_ON]-(dev:Developer)
            MATCH (dev)-[:CREATED]->(p:Project)<-[:BELONGS_TO]-(t)
            WHERE NOT EXISTS {
                MATCH (otherDev:Developer)-[:WORK_ON]->(:Task)-[:BELONGS_TO]->(p)
                WHERE otherDev <> dev
            }
            RETURN dev.id AS FraudsterDeveloper,
                   ent.id AS ComplicitEnterprise,
                   p.id AS ShellProject,
                   count(DISTINCT t) AS FakeTasksCompleted,
                   collect(t.id) AS CompromisedTaskIDs
            ORDER BY FakeTasksCompleted DESC
            """;

    private static final String ANOMALY_DETECTION_DEVELOPER_ENTERPRISE_SINGLE_QUERY = """
            MATCH (ent:Enterprise {id: $entId})-[:FINANCED]->(t:Task)<-[:WORK_ON]-(dev:Developer)
            MATCH (dev)-[:CREATED]->(p:Project)<-[:BELONGS_TO]-(t)
            WHERE NOT EXISTS {
                MATCH (otherDev:Developer)-[:WORK_ON]->(:Task)-[:BELONGS_TO]->(p)
                WHERE otherDev <> dev
            }
            RETURN dev.id AS FraudsterDeveloper,
                   ent.id AS ComplicitEnterprise,
                   p.id AS ShellProject,
                   count(DISTINCT t) AS FakeTasksCompleted,
                   collect(t.id) AS CompromisedTaskIDs
            ORDER BY FakeTasksCompleted DESC
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

    public List<CrossEnterpriseAnomalyDTO> detectCrossEnterpriseAnomalies() {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            return session.executeRead(tx -> {
                Result result = tx.run(ANOMALY_DETECTION_CROSS_ENTERPRISE_QUERY);
                return mapCrossEnterpriseAnomalies(result);
            });
        }
    }

    public List<CrossEnterpriseAnomalyDTO> detectCrossEnterpriseAnomaliesForEnterprise(String enterpriseId) {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            return session.executeRead(tx -> {
                Result result = tx.run(ANOMALY_DETECTION_CROSS_ENTERPRISE_SINGLE_QUERY, Map.of("entId", enterpriseId));
                return mapCrossEnterpriseAnomalies(result);
            });
        }
    }

    public List<DeveloperEnterpriseAnomalyDTO> detectDeveloperEnterpriseAnomalies() {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            return session.executeRead(tx -> {
                Result result = tx.run(ANOMALY_DETECTION_DEVELOPER_ENTERPRISE_QUERY);
                return mapDeveloperEnterpriseAnomalies(result);
            });
        }
    }

    public List<DeveloperEnterpriseAnomalyDTO> detectDeveloperEnterpriseAnomaliesForEnterprise(String enterpriseId) {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            return session.executeRead(tx -> {
                Result result = tx.run(ANOMALY_DETECTION_DEVELOPER_ENTERPRISE_SINGLE_QUERY, Map.of("entId", enterpriseId));
                return mapDeveloperEnterpriseAnomalies(result);
            });
        }
    }

    private List<CrossEnterpriseAnomalyDTO> mapCrossEnterpriseAnomalies(Result result) {
        List<CrossEnterpriseAnomalyDTO> anomalies = new ArrayList<>();
        while (result.hasNext()) {
            Record record = result.next();
            anomalies.add(new CrossEnterpriseAnomalyDTO(
                    record.get("EnterpriseA").asString(),
                    record.get("TasksFinancedByA_in_B").asLong(),
                    record.get("EnterpriseB").asString(),
                    record.get("TasksFinancedByB_in_A").asLong()));
        }
        return anomalies;
    }

    private List<DeveloperEnterpriseAnomalyDTO> mapDeveloperEnterpriseAnomalies(Result result) {
        List<DeveloperEnterpriseAnomalyDTO> anomalies = new ArrayList<>();
        while (result.hasNext()) {
            Record record = result.next();
            anomalies.add(new DeveloperEnterpriseAnomalyDTO(
                    record.get("FraudsterDeveloper").asString(),
                    record.get("ComplicitEnterprise").asString(),
                    record.get("ShellProject").asString(),
                    record.get("FakeTasksCompleted").asLong(),
                    record.get("CompromisedTaskIDs").asList(Value::asString)));
        }
        return anomalies;
    }
}
