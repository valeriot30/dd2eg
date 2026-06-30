package com.dd2eg.backend.DTO;

import com.dd2eg.backend.utils.TaskStatus;
import lombok.Data;
import java.util.List;

@Data
public class OpenTaskDTO {
    private String id;
    private String description;
    private String title;
    private TaskStatus status;
    private String priority;
    private List<String> skills;
}