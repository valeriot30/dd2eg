package com.dd2eg.backend.DTO;

import com.dd2eg.backend.utils.TaskStatus;
import lombok.Data;

@Data
public class OpenTaskDTO {
    private String id;
    private String description;
    private String title;
    private String priority;
}