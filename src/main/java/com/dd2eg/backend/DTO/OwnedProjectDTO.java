package com.dd2eg.backend.DTO;

import com.dd2eg.backend.utils.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class OwnedProjectDTO {
    private String id;
    private String description;
    private String name;
    private ProjectStatus status;
}
