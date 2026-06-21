package com.dd2eg.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
public class RecentProjectDTO {
    private String projectId;
    private String name;
    private Integer numContributors;
    private Integer totalFund;
}
