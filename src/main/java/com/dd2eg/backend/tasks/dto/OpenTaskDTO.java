package com.dd2eg.backend.tasks.dto;

import com.dd2eg.backend.tasks.TaskStatus;
import lombok.Data;

@Data
public class OpenTaskDTO {
    private String id;
    private String description;
    private String title;
    private TaskStatus status;
    private String priority;
}
