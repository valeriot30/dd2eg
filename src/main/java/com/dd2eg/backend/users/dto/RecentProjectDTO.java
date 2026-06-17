package com.dd2eg.backend.users.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecentProjectDTO {
    private String projectId;
    private String name;
    private Integer numContributors;
    private Integer totalFund;
}
