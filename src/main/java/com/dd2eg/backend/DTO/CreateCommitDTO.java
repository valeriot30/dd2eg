package com.dd2eg.backend.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommitDTO {

    private String hash;

    private String comment;

    private Integer numLines;
}
