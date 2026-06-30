package com.dd2eg.backend.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Commit {

    private String id;

    private String hash;

    private String authorId;

    private String authorUsername;

    private String comment;

    private Integer numLines;
}
