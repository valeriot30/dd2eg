package com.dd2eg.backend.DTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO {
    private String id;
    private String name;
    private String description;
    private String ownerName;

    private List<OpenTaskDTO> openTasks;

    private Long totalActiveContributors;
    private Double avgFirstResponseTimeInHours;
    private Double avgResolutionTimeInHours;
    private Double avgContributionsPerTask;
}