package com.dd2eg.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

/**
 * DTO per i risultati della Query 3 — Task Ranking.
 * Ordina i task aperti di un progetto in base alla compatibilità
 * con le skill del Developer.
 */
@Getter
@AllArgsConstructor
public class TaskRankingDTO {
    private final String taskId; // ID del task
    private final long priority; // priorità del task
    private final List<String> matchedSkills; // lista delle skill matchate
    private final long matchScore; // punteggio di compatibilità delle skill
}
