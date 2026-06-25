package com.dd2eg.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DTO per i risultati della Anomaly Detection: Cross-Enterprise.
 * Identifica cicli sospetti in cui l'Enterprise A finanzia task nel progetto
 * creato dall'Enterprise B, e viceversa.
 */
@Getter
@AllArgsConstructor
public class CrossEnterpriseAnomalyDTO {
    private final String enterpriseA;
    private final long tasksFinancedByAInB;
    private final String enterpriseB;
    private final long tasksFinancedByBInA;
}
