package com.dd2eg.backend.tasks;

import com.dd2eg.backend.projects.Project;
import com.dd2eg.backend.users.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.List;

@Setter
@Getter
public class Task {

    @Id
    private String id;

    private String title;

    private String description;

    private String body;

    private TaskStatus status;

    private Integer priority;

    private Integer numMaxCommits;

    private String projectId;

    private List<User> sponsorships;
}
