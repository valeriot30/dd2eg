package com.dd2eg.backend.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class CreateCommitDTO {

    @NotBlank(message = "Hash of the comment cannot be empty")
    private String hash;

    private String comment;

    private Integer numLines;
}
