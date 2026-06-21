package com.dd2eg.backend.service;

import com.dd2eg.backend.repository.Neo4jRecommendationRepository;
import com.dd2eg.backend.DTO.AnomalyScanResultDTO;
import com.dd2eg.backend.DTO.CrossEnterpriseAnomalyDTO;
import com.dd2eg.backend.DTO.DeveloperEnterpriseAnomalyDTO;
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
    public AnomalyScanResultDTO detectForEnterprise(String enterpriseId) {
        log.info("[AnomalyDetection] Running detection for Enterprise: {}", enterpriseId);
        List<CrossEnterpriseAnomalyDTO> crossAnomalies = neo4jRepo.detectCrossEnterpriseAnomaliesForEnterprise(enterpriseId);
        List<DeveloperEnterpriseAnomalyDTO> devAnomalies = neo4jRepo.detectDeveloperEnterpriseAnomaliesForEnterprise(enterpriseId);
        AnomalyScanResultDTO result = new AnomalyScanResultDTO();
        result.setCrossEnterpriseAnomalies(crossAnomalies);
        result.setDeveloperEnterpriseAnomalies(devAnomalies);
        return result;
    }

    /**
     * Runs anomaly detection for ALL enterprises (batch).
     * Can be triggered by admin for a full scan.
     *
     * return all anomalies found
     */
    public AnomalyScanResultDTO detectAll() {
        log.info("[AnomalyDetection] Running batch detection for all enterprises...");
        List<CrossEnterpriseAnomalyDTO> crossAnomalies = neo4jRepo.detectCrossEnterpriseAnomalies();
        List<DeveloperEnterpriseAnomalyDTO> devAnomalies = neo4jRepo.detectDeveloperEnterpriseAnomalies();
        AnomalyScanResultDTO result = new AnomalyScanResultDTO();
        result.setCrossEnterpriseAnomalies(crossAnomalies);
        result.setDeveloperEnterpriseAnomalies(devAnomalies);
        return result;
    }
}
