package com.dd2eg.backend.tasks.commits;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

@Getter
@Setter
public class Commit {

    @Indexed(unique = true)
    private String hash;

    private String comment;

    private Integer numLines;
}
