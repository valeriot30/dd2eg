package com.dd2eg.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

/**
 * Risultato aggregato della Anomaly Detection.
 * Aggrega i risultati delle due tipologie di anomalie (Cross-Enterprise e Developer-Enterprise)
 * e fornisce un conteggio totale tramite il metodo getTotalAnomalies().
 */
@Getter
@AllArgsConstructor
public class AnomalyScanResultDTO {
    private final List<CrossEnterpriseAnomalyDTO> crossEnterpriseAnomalies;
    private final List<DeveloperEnterpriseAnomalyDTO> developerEnterpriseAnomalies;

    public int getTotalAnomalies() {
        int crossCount = crossEnterpriseAnomalies != null ? crossEnterpriseAnomalies.size() : 0;
        int devCount = developerEnterpriseAnomalies != null ? developerEnterpriseAnomalies.size() : 0;
        return crossCount + devCount;
    }
}
