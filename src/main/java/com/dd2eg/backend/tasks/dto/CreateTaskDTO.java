package com.dd2eg.backend.tasks.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateTaskDTO {

    private String title;
    private String description;
    private String body;

    private Integer priority;
    private Integer numMaxCommits;

    private String projectId;
}