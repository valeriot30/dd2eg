package com.dd2eg.backend.neo4j;

import com.dd2eg.backend.neo4j.dto.AnomalyDetectionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for anomaly detection (Query 5 — Fraud Prevention).
 *
 * The primary detection is now event-driven: triggered automatically
 * by GraphSyncService after each FUNDING event is synced to Neo4j.
 *
 * This service remains available for on-demand detection by admins.
 */
@Service
public class AnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionService.class);

    private final Neo4jRecommendationRepository neo4jRepo;

    public AnomalyDetectionService(Neo4jRecommendationRepository neo4jRepo) {
        this.neo4jRepo = neo4jRepo;
    }

    /**
     * Runs anomaly detection for a specific Enterprise.
     * Used by GraphSyncService (event-driven) and by admin on-demand.
     *
     * param enterpriseId the Enterprise to check for suspicious cycles
     * return list of anomalies found
     */
    public List<AnomalyDetectionDTO> detectForEnterprise(String enterpriseId) {
        log.info("[AnomalyDetection] Running detection for Enterprise: {}", enterpriseId);
        return neo4jRepo.detectAnomaliesForEnterprise(enterpriseId);
    }

    /**
     * Runs anomaly detection for ALL enterprises (batch).
     * Can be triggered by admin for a full scan.
     *
     * return list of all anomalies found
     */
    public List<AnomalyDetectionDTO> detectAll() {
        log.info("[AnomalyDetection] Running batch detection for all enterprises...");
        return neo4jRepo.detectAnomalies();
    }
}
