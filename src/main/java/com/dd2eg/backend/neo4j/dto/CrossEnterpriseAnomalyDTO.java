package com.dd2eg.backend.neo4j.dto;

/**
 * DTO per i risultati della Anomaly Detection: Cross-Enterprise.
 * Identifica cicli sospetti in cui l'Enterprise A finanzia task nel progetto
 * creato dall'Enterprise B, e viceversa.
 */
public class CrossEnterpriseAnomalyDTO {
    private final String enterpriseA;
    private final long tasksFinancedByAInB;
    private final String enterpriseB;
    private final long tasksFinancedByBInA;

    public CrossEnterpriseAnomalyDTO(String enterpriseA, long tasksFinancedByAInB, String enterpriseB, long tasksFinancedByBInA) {
        this.enterpriseA = enterpriseA;
        this.tasksFinancedByAInB = tasksFinancedByAInB;
        this.enterpriseB = enterpriseB;
        this.tasksFinancedByBInA = tasksFinancedByBInA;
    }

    public String getEnterpriseA() {
        return enterpriseA;
    }

    public long getTasksFinancedByAInB() {
        return tasksFinancedByAInB;
    }

    public String getEnterpriseB() {
        return enterpriseB;
    }

    public long getTasksFinancedByBInA() {
        return tasksFinancedByBInA;
    }
}
