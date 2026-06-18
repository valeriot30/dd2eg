package com.dd2eg.backend.users.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DashboardProjectDTO {
    public String name;
    public String description;
    public Integer openTasks;
    public Integer numContributors;
}
