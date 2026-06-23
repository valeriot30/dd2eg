package com.dd2eg.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

/**
 * DTO per i risultati della Query 4 — Financing Recommendation per Enterprise.
 * Suggerisce progetti da finanziare basandosi sulle interest area dei progetti
 * precedentemente finanziati dall'Enterprise.
 */
@Getter
@AllArgsConstructor
public class FinancingRecommendationDTO {
    private final String recommendedProjectId; // ID del progetto da finanziare
    private final long sharedInterestAreaCount; // numero di interest area in comune
    private final List<String> matchingInterestAreas; // lista delle interest area in comune
    private final long availableTasks; // numero di task disponibili nel progetto
}
