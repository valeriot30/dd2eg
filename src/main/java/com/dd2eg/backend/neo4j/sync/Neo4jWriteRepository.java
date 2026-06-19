package com.dd2eg.backend.neo4j.sync;

import org.neo4j.driver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Repository for WRITE operations on Neo4j.
 * Uses MERGE to ensure idempotency: if an event is reprocessed,
 * no duplicate nodes or relationships are created.
 *
 * Separated from Neo4jRecommendationRepository which handles reads only.
 */
@Repository
public class Neo4jWriteRepository {

    private static final Logger log = LoggerFactory.getLogger(Neo4jWriteRepository.class);

    private final Driver driver;

    public Neo4jWriteRepository(Driver driver) {
        this.driver = driver;
    }

    /**
     * ADD_USER — Creates a Developer or Enterprise node with its Skills.
     * Dynamic label based on user type.
     *
     * Resulting graph:
     * (:Developer {id})-[:HAS_SKILL]->(:Skill {name})
     * (:Enterprise {id})
     */
    public void createUser(String userId, String userType, List<String> skillNames) {
        String label = "ENTERPRISE".equalsIgnoreCase(userType) ? "Enterprise" : "Developer";

        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            session.executeWrite(tx -> {
                // Create the user node with the correct label
                tx.run("MERGE (u:" + label + " {id: $userId})",
                        Map.of("userId", userId));

                // For Developers, create skills and HAS_SKILL relationships
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
     * ADD_PROJECT — Creates a Project node with its associated Tags.
     *
     * Resulting graph:
     * (:Developer {id})-[:CREATED]->(:Project {id, status})
     * (:Project {id, status})-[:CATEGORIZED_BY]->(:Tag {name})
     */
    public void createProject(String projectId, String creatorId, String status, List<String> tags) {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            session.executeWrite(tx -> {
                // Create the project node
                tx.run("MERGE (p:Project {id: $projectId}) SET p.status = $status",
                        Map.of("projectId", projectId, "status", status));

                // Create the CREATED relationship from the developer
                if (creatorId != null) {
                    tx.run("""
                            MATCH (d:Developer {id: $creatorId})
                            MATCH (p:Project {id: $projectId})
                            MERGE (d)-[:CREATED]->(p)
                            """,
                            Map.of("creatorId", creatorId, "projectId", projectId));
                }

                // Create tags and CATEGORIZED_BY relationships
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
        log.debug("[Neo4jWrite] Created Project node: {} by creator: {} with {} tags", projectId,
                creatorId, tags != null ? tags.size() : 0);
    }

    /**
     * ADD_TASK — Creates a Task node, links it to the Project, and creates required
     * Skills.
     *
     * Resulting graph:
     * (:Task {id, status})-[:BELONGS_TO]->(:Project {id})
     * (:Task {id})-[:REQUIRES_SKILL]->(:Skill {name})
     */
    public void createTask(String taskId, String projectId, List<String> skills) {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            session.executeWrite(tx -> {
                // Create the task node and link it to the project
                tx.run("""
                        MERGE (t:Task {id: $taskId})
                        SET t.status = 'open'
                        WITH t
                        MATCH (p:Project {id: $projectId})
                        MERGE (t)-[:BELONGS_TO]->(p)
                        """,
                        Map.of("taskId", taskId, "projectId", projectId));

                // Create required skills and REQUIRES_SKILL relationships
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
     * FUNDING — Creates a FINANCED relationship between Enterprise and Task.
     *
     * Resulting graph:
     * (:Enterprise {id})-[:FINANCED]->(:Task {id})
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
     * ADD_WORKER_TO_TASK — Creates a WORK_ON relationship between
     * Developer and Task.
     *
     * Resulting graph:
     * (:Developer {id})-[:WORK_ON]->(:Task {id})
     */
    public void addWorkerToTask(String developerId, String taskId) {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            session.executeWrite(tx -> {
                tx.run("""
                        MATCH (d:Developer {id: $developerId})
                        MATCH (t:Task {id: $taskId})
                        MERGE (d)-[:WORK_ON]->(t)
                        """,
                        Map.of("developerId", developerId, "taskId", taskId));
                return null;
            });
        }
        log.debug("[Neo4jWrite] Created WORK_ON relation: {} -> {}", developerId, taskId);
    }
}
