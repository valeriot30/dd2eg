package com.dd2eg.backend.sync;

import org.neo4j.driver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Repository per le operazioni di SCRITTURA su Neo4j.
 * Usa MERGE per garantire idempotenza: se un evento viene riprocessato,
 * non si creano nodi o relazioni duplicati.
 *
 * Separato dal Neo4jRecommendationRepository che gestisce solo le letture.
 */
@Repository
public class Neo4jWriteRepository {

    private static final Logger log = LoggerFactory.getLogger(Neo4jWriteRepository.class);

    private final Driver driver;

    public Neo4jWriteRepository(Driver driver) {
        this.driver = driver;
    }

    /**
     * ADD_USER — Crea nodo Developer o Enterprise con le sue Skill.
     * Label dinamica basata sul tipo utente.
     *
     * Grafo risultante:
     *   (:Developer {id})-[:HAS_SKILL]->(:Skill {name})
     *   (:Enterprise {id})
     */
    public void createUser(String userId, String userType, List<String> skillNames) {
        String label = "ENTERPRISE".equalsIgnoreCase(userType) ? "Enterprise" : "Developer";

        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            session.executeWrite(tx -> {
                // Crea il nodo utente con il label corretto
                tx.run("MERGE (u:" + label + " {id: $userId})",
                        Map.of("userId", userId));

                // Per i Developer, crea le skill e le relazioni HAS_SKILL
                if ("Developer".equals(label) && skillNames != null) {
                    for (String skillName : skillNames) {
                        tx.run("""
                                MATCH (u:Developer {id: $userId})
                                MERGE (s:Skill {name: $skillName})
                                MERGE (u)-[:HAS_SKILL]->(s)
                                """,
                                Map.of("userId", userId, "skillName", skillName));
                    }
                }
                return null;
            });
        }
        log.debug("[Neo4jWrite] Created {} node: {}", label, userId);
    }

    /**
     * ADD_PROJECT — Crea nodo Project con i Tag associati.
     *
     * Grafo risultante:
     *   (:Project {id, status})-[:CATEGORIZED_BY]->(:Tag {name})
     */
    public void createProject(String projectId, String status, List<String> tags) {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            session.executeWrite(tx -> {
                // Crea il nodo progetto
                tx.run("MERGE (p:Project {id: $projectId}) SET p.status = $status",
                        Map.of("projectId", projectId, "status", status));

                // Crea i tag e le relazioni CATEGORIZED_BY
                if (tags != null) {
                    for (String tagName : tags) {
                        tx.run("""
                                MATCH (p:Project {id: $projectId})
                                MERGE (t:Tag {name: $tagName})
                                MERGE (p)-[:CATEGORIZED_BY]->(t)
                                """,
                                Map.of("projectId", projectId, "tagName", tagName));
                    }
                }
                return null;
            });
        }
        log.debug("[Neo4jWrite] Created Project node: {} with {} tags", projectId,
                tags != null ? tags.size() : 0);
    }

    /**
     * ADD_TASK — Crea nodo Task, lo collega al Project, e crea le Skill richieste.
     *
     * Grafo risultante:
     *   (:Task {id, status})-[:BELONGS_TO]->(:Project {id})
     *   (:Task {id})-[:REQUIRES_SKILL]->(:Skill {name})
     */
    public void createTask(String taskId, String projectId, List<String> skills) {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            session.executeWrite(tx -> {
                // Crea il nodo task e collegalo al progetto
                tx.run("""
                        MERGE (t:Task {id: $taskId})
                        SET t.status = 'open'
                        WITH t
                        MATCH (p:Project {id: $projectId})
                        MERGE (t)-[:BELONGS_TO]->(p)
                        """,
                        Map.of("taskId", taskId, "projectId", projectId));

                // Crea le skill richieste e le relazioni REQUIRES_SKILL
                if (skills != null) {
                    for (String skillName : skills) {
                        tx.run("""
                                MATCH (t:Task {id: $taskId})
                                MERGE (s:Skill {name: $skillName})
                                MERGE (t)-[:REQUIRES_SKILL]->(s)
                                """,
                                Map.of("taskId", taskId, "skillName", skillName));
                    }
                }
                return null;
            });
        }
        log.debug("[Neo4jWrite] Created Task node: {} in Project: {}", taskId, projectId);
    }

    /**
     * FUNDING — Crea relazione FINANCED tra Enterprise e Task.
     *
     * Grafo risultante:
     *   (:Enterprise {id})-[:FINANCED]->(:Task {id})
     */
    public void createFunding(String enterpriseId, String taskId) {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            session.executeWrite(tx -> {
                tx.run("""
                        MATCH (e:Enterprise {id: $enterpriseId})
                        MATCH (t:Task {id: $taskId})
                        MERGE (e)-[:FINANCED]->(t)
                        """,
                        Map.of("enterpriseId", enterpriseId, "taskId", taskId));
                return null;
            });
        }
        log.debug("[Neo4jWrite] Created FINANCED relation: {} -> {}", enterpriseId, taskId);
    }

    /**
     * ADD_CONTRIBUTOR_TO_PROJECT — Crea relazione CONTRIBUTED_TO tra Developer e Project.
     *
     * Grafo risultante:
     *   (:Developer {id})-[:CONTRIBUTED_TO]->(:Project {id})
     */
    public void addContributorToProject(String developerId, String projectId) {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            session.executeWrite(tx -> {
                tx.run("""
                        MATCH (d:Developer {id: $developerId})
                        MATCH (p:Project {id: $projectId})
                        MERGE (d)-[:CONTRIBUTED_TO]->(p)
                        """,
                        Map.of("developerId", developerId, "projectId", projectId));
                return null;
            });
        }
        log.debug("[Neo4jWrite] Created CONTRIBUTED_TO relation: {} -> {}", developerId, projectId);
    }
}
