package com.dd2eg.backend.tasks.commits;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Sharded;

@Getter
@Setter
@Document(collection = "commits")
@Sharded(shardKey = { "taskId", "_id" })
public class Commit {

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    private String hash;

    private String taskId;

    private String projectId;

    private String authorId;

    private String authorUsername;

    private String comment;

    private Integer numLines;
}
