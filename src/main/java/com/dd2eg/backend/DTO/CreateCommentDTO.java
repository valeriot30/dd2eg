package com.dd2eg.backend.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentDTO {
    @NotBlank(message = "Content of the comment cannot be empty")
    private String content;
}
