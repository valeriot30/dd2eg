package com.dd2eg.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DTO per i risultati della Query 2 — Skills Recommendation.
 * Suggerisce al Developer quali skill dovrebbe imparare
 * basandosi sulla frequenza di richiesta nei task aperti correlati.
 */
@Getter
@AllArgsConstructor
public class SkillRecommendationDTO {
    private final String recommendedSkill; // skill raccomandata
    private final long frequency; // quante volte quella skill è richiesta nei task aperti
}
