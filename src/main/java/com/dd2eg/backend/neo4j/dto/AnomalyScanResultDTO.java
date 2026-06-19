package com.dd2eg.backend.neo4j.dto;

import java.util.List;

/**
 * Risultato aggregato della Anomaly Detection.
 */
public class AnomalyScanResultDTO {
    private final List<CrossEnterpriseAnomalyDTO> crossEnterpriseAnomalies;
    private final List<DeveloperEnterpriseAnomalyDTO> developerEnterpriseAnomalies;

    public AnomalyScanResultDTO(List<CrossEnterpriseAnomalyDTO> crossEnterpriseAnomalies, List<DeveloperEnterpriseAnomalyDTO> developerEnterpriseAnomalies) {
        this.crossEnterpriseAnomalies = crossEnterpriseAnomalies;
        this.developerEnterpriseAnomalies = developerEnterpriseAnomalies;
    }

    public List<CrossEnterpriseAnomalyDTO> getCrossEnterpriseAnomalies() {
        return crossEnterpriseAnomalies;
    }

    public List<DeveloperEnterpriseAnomalyDTO> getDeveloperEnterpriseAnomalies() {
        return developerEnterpriseAnomalies;
    }

    public int getTotalAnomalies() {
        return crossEnterpriseAnomalies.size() + developerEnterpriseAnomalies.size();
    }
}
