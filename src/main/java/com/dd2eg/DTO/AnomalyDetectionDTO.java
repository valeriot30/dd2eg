package com.dd2eg.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

/**
 * DTO per i risultati della Query 5 — Anomaly Detection (Fraud Prevention).
 * Identifica cicli sospetti dove un Developer lavora su task finanziati
 * da un'Enterprise che a sua volta finanzia task nel progetto creato dal
 * Developer stesso.
 */
@Getter
@AllArgsConstructor
public class AnomalyDetectionDTO {
    private final String enterpriseId; // ID dell'Enterprise coinvolta nel ciclo
    private final String suspiciousDeveloperId; // ID del developer sospetto
    private final long cycleFrequency; // quante volte si è verificato questo ciclo
    private final List<String> tasksWorked; // lista dei task su cui ha lavorato il developer sospetto
    private final List<String> financedTasksInTheirProject; // lista dei task finanziati dall'enterprise
}
