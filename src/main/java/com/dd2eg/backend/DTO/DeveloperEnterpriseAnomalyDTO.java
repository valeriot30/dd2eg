package com.dd2eg.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

/**
 * DTO per i risultati della Anomaly Detection: Developer-Enterprise.
 * Identifica shell projects creati da un Developer per farsi finanziare
 * task fittizi da una Enterprise complice, dove nessun altro developer lavora.
 */
@Getter
@AllArgsConstructor
public class DeveloperEnterpriseAnomalyDTO {
    private final String fraudsterDeveloperId;
    private final String complicitEnterpriseId;
    private final String shellProjectId;
    private final long fakeTasksCompleted;
    private final List<String> compromisedTaskIds;
}
