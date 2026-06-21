package com.dd2eg.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

/**
 * DTO per i risultati della Query 4 — Financing Recommendation per Enterprise.
 * Suggerisce progetti da finanziare basandosi sui tag dei progetti
 * precedentemente finanziati dall'Enterprise.
 */
@Getter
@AllArgsConstructor
public class FinancingRecommendationDTO {
    private final String recommendedProjectId; // ID del progetto da finanziare
    private final long sharedTagCount; // numero di tag in comune
    private final List<String> matchingTags; // lista dei tag in comune
    private final long availableTasks; // numero di task disponibili nel progetto
}
