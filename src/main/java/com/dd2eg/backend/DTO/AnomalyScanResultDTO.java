package com.dd2eg.backend.DTO;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * Risultato aggregato della Anomaly Detection.
 */
@Setter
@Getter
public class AnomalyScanResultDTO {
    private List<CrossEnterpriseAnomalyDTO> crossEnterpriseAnomalies;
    private List<DeveloperEnterpriseAnomalyDTO> developerEnterpriseAnomalies;

    public int getTotalAnomalies() {
        int crossCount = crossEnterpriseAnomalies != null ? crossEnterpriseAnomalies.size() : 0;
        int devCount = developerEnterpriseAnomalies != null ? developerEnterpriseAnomalies.size() : 0;
        return crossCount + devCount;
    }
}
