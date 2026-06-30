package com.dd2eg.DTO;

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