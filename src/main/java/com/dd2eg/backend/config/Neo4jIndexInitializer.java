package com.dd2eg.backend.config;

import jakarta.annotation.PostConstruct;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Neo4jIndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(Neo4jIndexInitializer.class);
    private final Driver neo4jDriver;

    public Neo4jIndexInitializer(Driver neo4jDriver) {
        this.neo4jDriver = neo4jDriver;
    }

    @PostConstruct
    public void initializeIndexes() {
        log.info("Initializing Neo4j Constraints and Indexes...");

        List<String> indexQueries = List.of(
            // Node Key Constraints (Unique Indexes)
            "CREATE CONSTRAINT dev_id IF NOT EXISTS FOR (d:Developer) REQUIRE d.id IS UNIQUE",
            "CREATE CONSTRAINT ent_id IF NOT EXISTS FOR (e:Enterprise) REQUIRE e.id IS UNIQUE",
            "CREATE CONSTRAINT proj_id IF NOT EXISTS FOR (p:Project) REQUIRE p.id IS UNIQUE",
            "CREATE CONSTRAINT task_id IF NOT EXISTS FOR (t:Task) REQUIRE t.id IS UNIQUE",

            // Skill and Tag Name Indexes
            "CREATE CONSTRAINT skill_name IF NOT EXISTS FOR (s:Skill) REQUIRE s.name IS UNIQUE",
            "CREATE CONSTRAINT tag_name IF NOT EXISTS FOR (t:Tag) REQUIRE t.name IS UNIQUE",

            // Status Property Indexes
            "CREATE INDEX proj_status IF NOT EXISTS FOR (p:Project) ON (p.status)",
            "CREATE INDEX task_status IF NOT EXISTS FOR (t:Task) ON (t.status)"
        );

        try (Session session = neo4jDriver.session()) {
            for (String query : indexQueries) {
                try {
                    session.executeWrite(tx -> {
                        tx.run(query);
                        return null;
                    });
                } catch (Exception e) {
                    log.warn("Failed to execute index query (it might already exist with a different name): " + query, e.getMessage());
                }
            }
            log.info("Neo4j Constraints and Indexes initialized successfully.");
        } catch (Exception e) {
            log.error("Failed to connect to Neo4j to initialize indexes", e);
        }
    }
}
