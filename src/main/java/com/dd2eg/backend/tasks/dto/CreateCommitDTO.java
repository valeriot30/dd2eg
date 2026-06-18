package com.dd2eg.backend.tasks.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommitDTO {

    private String hash;

    private String comment;

    private Integer numLines;
}
