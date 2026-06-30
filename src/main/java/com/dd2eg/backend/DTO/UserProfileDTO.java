package com.dd2eg.backend.DTO;

import com.dd2eg.backend.model.DeveloperInfo;
import com.dd2eg.backend.model.EnterpriseInfo;
import com.dd2eg.backend.model.Project;
import com.dd2eg.backend.utils.UserType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserProfileDTO {
    private String id;
    private String username;
    private String profilePic;
    private UserType userType;
    private Double rating;
    private List<String> skills;
    private List<String> interestAreas;
    private List<Project> ownedProjects;
    private List<RecentProjectDTO> lastProjects;
    private List<RecentProjectDTO> lastContributions;
    private DeveloperInfo developerInfo;
    private EnterpriseInfo enterpriseInfo;
}
