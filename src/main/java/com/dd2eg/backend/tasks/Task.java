package com.dd2eg.backend.tasks;

import com.dd2eg.backend.tasks.comments.Comment;
import com.dd2eg.backend.tasks.commits.Commit;
import com.dd2eg.backend.users.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Sharded;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Document(collection = "tasks")
@Sharded(shardKey = { "projectId", "_id" })
public class Task {

    @Id
    private String id;

    private String title;

    private String description;

    private String body;

    @Indexed(partialFilter = "{ status: 'OPEN' }")
    private TaskStatus status;

    private String priority;

    private Integer numMaxCommits;

    private String projectId;

    private Integer budget;

    // we save name of the enterprise and the amount
    // amount < budget
    private List<User> sponsorships;

    private List<String> skills;

    //TODO pre-allocation of commits of numMaxCommits
    private List<Commit> commits;

    private List<Comment> comments = new ArrayList<>();
}
