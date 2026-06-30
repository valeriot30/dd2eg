package com.dd2eg.backend.controller;

import com.dd2eg.backend.DTO.FinancingRecommendationDTO;
import com.dd2eg.backend.DTO.ProjectRecommendationDTO;
import com.dd2eg.backend.DTO.SkillRecommendationDTO;
import com.dd2eg.backend.DTO.TaskRankingDTO;
import com.dd2eg.backend.service.RecommendationService;
import com.dd2eg.backend.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST per le API di raccomandazione basate su Neo4j.
 * Espone le Query 1-4 come endpoint sincroni.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
@Tag(name = "Recommendations", description = "Neo4j Graph-based Recommendation API")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(
            summary = "Get Project Recommendations (Q1)",
            description = "Returns optimal project recommendations for the logged-in Developer based on skill matching and graph topology."
    )
    @ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectRecommendationDTO>> getProjectRecommendations(
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(recommendationService.getProjectRecommendations(currentUser.getId()));
    }

    @Operation(
            summary = "Get Skill Recommendations (Q2)",
            description = "Suggests new skills for the logged-in Developer to learn, based on trending market demands and their current network."
    )
    @ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping("/skills")
    public ResponseEntity<List<SkillRecommendationDTO>> getSkillRecommendations(
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(recommendationService.getSkillRecommendations(currentUser.getId()));
    }

    @Operation(
            summary = "Get Task Ranking for Project (Q3)",
            description = "Ranks the available tasks within a specific project based on how well they match the logged-in Developer's skill set."
    )
    @ApiResponse(responseCode = "200", description = "Task ranking retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid Project ID")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping("/tasks")
    public ResponseEntity<List<TaskRankingDTO>> getTaskRanking(
            @Parameter(description = "The ID of the project to rank tasks for", required = true)
            @RequestParam String projId,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(recommendationService.getTaskRanking(projId, currentUser.getId()));
    }

    @Operation(
            summary = "Get Financing Recommendations (Q4)",
            description = "Recommends high-potential projects for financial sponsorship to the logged-in Enterprise user."
    )
    @ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping("/financing")
    public ResponseEntity<List<FinancingRecommendationDTO>> getFinancingRecommendations(
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(recommendationService.getFinancingRecommendations(currentUser.getId()));
    }
}