package com.dd2eg.backend.users.dto;

import com.dd2eg.backend.neo4j.dto.SkillRecommendationDTO;
import com.dd2eg.backend.projects.dto.TopContributorDTO;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeveloperStatsDTO {

    private List<RecentProjectDTO> trendingProjects = new ArrayList<>();

    private List<SkillRecommendationDTO> trendingSkills;

    private List<TopContributorDTO> topContributors;
}