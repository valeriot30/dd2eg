package com.dd2eg.backend.neo4j.dto;

import java.util.List;

/**
 * DTO per i risultati della Anomaly Detection: Developer-Enterprise.
 * Identifica shell projects creati da un Developer per farsi finanziare
 * task fittizi da una Enterprise complice, dove nessun altro developer lavora.
 */
public class DeveloperEnterpriseAnomalyDTO {
    private final String fraudsterDeveloperId;
    private final String complicitEnterpriseId;
    private final String shellProjectId;
    private final long fakeTasksCompleted;
    private final List<String> compromisedTaskIds;

    public DeveloperEnterpriseAnomalyDTO(String fraudsterDeveloperId, String complicitEnterpriseId, String shellProjectId, long fakeTasksCompleted, List<String> compromisedTaskIds) {
        this.fraudsterDeveloperId = fraudsterDeveloperId;
        this.complicitEnterpriseId = complicitEnterpriseId;
        this.shellProjectId = shellProjectId;
        this.fakeTasksCompleted = fakeTasksCompleted;
        this.compromisedTaskIds = compromisedTaskIds;
    }

    public String getFraudsterDeveloperId() {
        return fraudsterDeveloperId;
    }

    public String getComplicitEnterpriseId() {
        return complicitEnterpriseId;
    }

    public String getShellProjectId() {
        return shellProjectId;
    }

    public long getFakeTasksCompleted() {
        return fakeTasksCompleted;
    }

    public List<String> getCompromisedTaskIds() {
        return compromisedTaskIds;
    }
}
