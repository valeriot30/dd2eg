package com.dd2eg.controller;

import com.dd2eg.backend.neo4j.dto.*;
import com.dd2eg.DTO.FinancingRecommendationDTO;
import com.dd2eg.DTO.ProjectRecommendationDTO;
import com.dd2eg.DTO.SkillRecommendationDTO;
import com.dd2eg.DTO.TaskRankingDTO;
import com.dd2eg.repository.Neo4jRecommendationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST per le API di raccomandazione basate su Neo4j.
 * Espone le Query 1-4 come endpoint sincroni.
 * La Query 5 (Anomaly Detection) è gestita dal AnomalyDetectionService come job
 * schedulato.
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    //TODO ADD SERVICE FOR RECOMMENDATION

    private final Neo4jRecommendationRepository neo4jRepo;

    public RecommendationController(Neo4jRecommendationRepository neo4jRepo) {
        this.neo4jRepo = neo4jRepo;
    }

    /**
     * Q1 — Progetti raccomandati per un Developer.
     * GET /api/recommendations/projects?devId=xxx
     */
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectRecommendationDTO>> getProjectRecommendations(
            @RequestParam String devId) {
        List<ProjectRecommendationDTO> results = neo4jRepo.getProjectRecommendations(devId);
        return ResponseEntity.ok(results);
    }

    /**
     * Q2 — Skill raccomandate per un Developer.
     * GET /api/recommendations/skills?devId=xxx
     */
    @GetMapping("/skills")
    public ResponseEntity<List<SkillRecommendationDTO>> getSkillRecommendations(
            @RequestParam String devId) {
        List<SkillRecommendationDTO> results = neo4jRepo.getSkillRecommendations(devId);
        return ResponseEntity.ok(results);
    }

    /**
     * Q3 — Ranking dei task ottimali in un progetto per un Developer.
     * GET /api/recommendations/tasks?projId=xxx&devId=yyy
     */
    @GetMapping("/tasks")
    public ResponseEntity<List<TaskRankingDTO>> getTaskRanking(
            @RequestParam String projId,
            @RequestParam String devId) {
        List<TaskRankingDTO> results = neo4jRepo.getTaskRanking(projId, devId);
        return ResponseEntity.ok(results);
    }

    /**
     * Q4 — Progetti raccomandati per il finanziamento da parte di un'Enterprise.
     * GET /api/recommendations/financing?entId=xxx
     */
    @GetMapping("/financing")
    public ResponseEntity<List<FinancingRecommendationDTO>> getFinancingRecommendations(
            @RequestParam String entId) {
        List<FinancingRecommendationDTO> results = neo4jRepo.getFinancingRecommendations(entId);
        return ResponseEntity.ok(results);
    }
}
