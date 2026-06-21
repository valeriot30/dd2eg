package com.dd2eg.backend.DTO;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO per i risultati della Anomaly Detection: Cross-Enterprise.
 * Identifica cicli sospetti in cui l'Enterprise A finanzia task nel progetto
 * creato dall'Enterprise B, e viceversa.
 */
@Setter
@Getter
public class CrossEnterpriseAnomalyDTO {
    private String enterpriseA;
    private long tasksFinancedByAInB;
    private String enterpriseB;
    private long tasksFinancedByBInA;
}
