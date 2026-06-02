package com.dd2eg.backend.neo4j;

import com.dd2eg.backend.neo4j.dto.AnomalyDetectionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service per l'esecuzione schedulata della Query 5 — Anomaly Detection.
 *
 * Non è esposta come API REST sincrona.
 * Viene eseguita in background ogni (6?) ore tramite @Scheduled.
 */
@Service
public class AnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionService.class);

    private final Neo4jRecommendationRepository neo4jRepo;

    // TODO: Fare anche un repository MongoDB per persistere gli alert
    // private final AnomalyAlertMongoRepository alertRepository;

    public AnomalyDetectionService(Neo4jRecommendationRepository neo4jRepo) {
        this.neo4jRepo = neo4jRepo;
    }

    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // esecuizione schedulata ogni 6 ore
    public void runScheduledAnomalyDetection() {
        log.info("[AnomalyDetection] Starting scheduled scan...");

        List<String> enterpriseIds = List.of(); // sostituire!!!

        for (String entId : enterpriseIds) {
            try {
                List<AnomalyDetectionDTO> anomalies = neo4jRepo.detectAnomalies();

                if (!anomalies.isEmpty()) {
                    log.warn("[AnomalyDetection] Enterprise {} — Found {} suspicious developers",
                            entId, anomalies.size());

                    for (AnomalyDetectionDTO anomaly : anomalies) {
                        log.warn("  → Dev: {}, CycleFrequency: {}, TasksWorked: {}, FinancedTasks: {}",
                                anomaly.getSuspiciousDeveloperId(),
                                anomaly.getCycleFrequency(),
                                anomaly.getTasksWorked(),
                                anomaly.getFinancedTasksInTheirProject());
                    }

                    // TODO: Persistere su MongoDB collection "anomaly_alerts"
                    // alertRepository.saveAll(toAlertDocuments(entId, anomalies));
                }
            } catch (Exception e) {
                log.error("[AnomalyDetection] Error for Enterprise {}: {}", entId, e.getMessage(), e);
            }
        }

        log.info("[AnomalyDetection] Scan completed.");
    }

    /**
     * Metodo pubblico per eseguire l'anomaly detection on-demand per una singola
     * Enterprise.
     * Usato internamente (es. da admin), mai come API pubblica sincrona.
     */
    public List<AnomalyDetectionDTO> detectForEnterprise(String entId) {
        return neo4jRepo.detectAnomalies();
    }
}
