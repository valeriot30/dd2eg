package com.dd2eg.backend.neo4j.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * DTO per i risultati della Anomaly Detection: Developer-Enterprise.
 * Identifica shell projects creati da un Developer per farsi finanziare
 * task fittizi da una Enterprise complice, dove nessun altro developer lavora.
 */
@Setter
@Getter
public class DeveloperEnterpriseAnomalyDTO {
    private String fraudsterDeveloperId;
    private String complicitEnterpriseId;
    private String shellProjectId;
    private long fakeTasksCompleted;
    private List<String> compromisedTaskIds;
}
