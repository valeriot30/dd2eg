package com.dd2eg.backend.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateTaskDTO {
    private List<String> skills;
}
