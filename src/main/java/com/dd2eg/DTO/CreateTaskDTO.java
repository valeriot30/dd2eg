package com.dd2eg.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class CreateTaskDTO {

    private String title;
    private String description;
    private String body;

    private String priority;
    private Integer numMaxCommits;

    private String projectId;

    private List<String> skills;
}