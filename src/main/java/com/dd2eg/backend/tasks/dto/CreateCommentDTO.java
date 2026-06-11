package com.dd2eg.backend.tasks.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentDTO {
    @NotBlank(message = "Content of the comment cannotbe empty")
    private String content;
}
