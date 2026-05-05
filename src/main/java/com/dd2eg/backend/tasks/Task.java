package com.dd2eg.backend.tasks;

import com.dd2eg.backend.projects.Project;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Setter
@Getter
public class Task {

    @Id
    private String Id;

    private String title;

    private String body;

    private TaskStatus status;

    private Integer priority;

    private Integer numMaxCommits;

    private Project project;
}
