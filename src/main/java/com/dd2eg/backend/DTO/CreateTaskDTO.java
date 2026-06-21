package com.dd2eg.backend.DTO;

import com.dd2eg.backend.utils.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Data
public class CreateTaskDTO {

    @NotBlank(message = "Title of the task cannot be empty")
    private String title;

    @NotBlank(message = "Description of the task cannot be empty")
    private String description;

    @NotBlank(message = "Body of the task cannot be empty")
    private String body;

    private String priority = TaskPriority.MEDIUM.name();
    private Integer numMaxCommits;

    private String projectId;

    private List<String> skills;
}