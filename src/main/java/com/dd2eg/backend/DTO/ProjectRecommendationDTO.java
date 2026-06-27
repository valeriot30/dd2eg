package com.dd2eg.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

/**
 * DTO per i risultati della Query 1 — Project Recommendation per Developer.
 * Contiene l'ID del progetto raccomandato (riferimento a MongoDB),
 * il conteggio delle interest area condivise, i task aperti compatibili e le skill
 * matchate.
 */
@Getter
@AllArgsConstructor
public class ProjectRecommendationDTO {
    private final String recommendedProjectId; // ID del progetto raccomandato
    private final long sharedInterestAreaCount; // numero di interest area in comune
    private final long openMatchingTasks; // numero di task aperti compatibili
    private final List<String> matchingSkills; // lista delle skill matchate
}
