package com.dd2eg.backend.tasks.commits;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

@Getter
@Setter
public class Commit {

    @Id
    private String id;

    @Indexed(unique = true)
    private String hash;

    private String taskId;

    private String projectId;

    private String authorId;

    private String authorUsername;

    private String comment;

    private Integer numLines;
}
