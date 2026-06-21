package com.dd2eg.backend.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnterpriseStatsDTO {
    private int openedTasks;
    private int completedTasks;
    private int totalBudgetSpent;
    private int totalContributions;
    private int uniqueDevelopersInvolved;
}
