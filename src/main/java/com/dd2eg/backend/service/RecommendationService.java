package com.dd2eg.backend.service;

import com.dd2eg.backend.DTO.FinancingRecommendationDTO;
import com.dd2eg.backend.DTO.ProjectRecommendationDTO;
import com.dd2eg.backend.DTO.SkillRecommendationDTO;
import com.dd2eg.backend.DTO.TaskRankingDTO;
import com.dd2eg.backend.repository.Neo4jRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final Neo4jRecommendationRepository neo4jRepo;

    public List<ProjectRecommendationDTO> getProjectRecommendations(String devId) {
        return neo4jRepo.getProjectRecommendations(devId);
    }

    public List<SkillRecommendationDTO> getSkillRecommendations(String devId) {
        return neo4jRepo.getSkillRecommendations(devId);
    }

    public List<TaskRankingDTO> getTaskRanking(String projId, String devId) {
        return neo4jRepo.getTaskRanking(projId, devId);
    }

    public List<FinancingRecommendationDTO> getFinancingRecommendations(String entId) {
        return neo4jRepo.getFinancingRecommendations(entId);
    }
}