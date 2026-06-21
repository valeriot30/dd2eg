package com.dd2eg.backend.controller;

import com.dd2eg.backend.DTO.FinancingRecommendationDTO;
import com.dd2eg.backend.DTO.ProjectRecommendationDTO;
import com.dd2eg.backend.DTO.SkillRecommendationDTO;
import com.dd2eg.backend.DTO.TaskRankingDTO;
import com.dd2eg.backend.service.RecommendationService;
import com.dd2eg.backend.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST per le API di raccomandazione basate su Neo4j.
 * Espone le Query 1-4 come endpoint sincroni.
 * La Query 5 (Anomaly Detection) è gestita dal AnomalyDetectionService come job
 * schedulato.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * Q1 — Progetti raccomandati per il Developer loggato.
     * GET /api/recommendations/projects
     */
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectRecommendationDTO>> getProjectRecommendations(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(recommendationService.getProjectRecommendations(currentUser.getId()));
    }

    /**
     * Q2 — Skill raccomandate per il Developer loggato.
     * GET /api/recommendations/skills
     */
    @GetMapping("/skills")
    public ResponseEntity<List<SkillRecommendationDTO>> getSkillRecommendations(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(recommendationService.getSkillRecommendations(currentUser.getId()));
    }

    /**
     * Q3 — Ranking dei task ottimali in un progetto per il Developer loggato.
     * GET /api/recommendations/tasks?projId=xxx
     */
    @GetMapping("/tasks")
    public ResponseEntity<List<TaskRankingDTO>> getTaskRanking(
            @RequestParam String projId,
            @AuthenticationPrincipal User currentUser) {
        // projId rimane come RequestParam perché specifica di quale progetto stiamo chiedendo il ranking,
        // mentre devId viene estratto in modo sicuro dal token.
        return ResponseEntity.ok(recommendationService.getTaskRanking(projId, currentUser.getId()));
    }

    /**
     * Q4 — Progetti raccomandati per il finanziamento da parte dell'Enterprise loggata.
     * GET /api/recommendations/financing
     */
    @GetMapping("/financing")
    public ResponseEntity<List<FinancingRecommendationDTO>> getFinancingRecommendations(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(recommendationService.getFinancingRecommendations(currentUser.getId()));
    }
}