package com.dd2eg.backend.projects.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateProjectDTO
{
    private String name;
    private String description;
    private List<String> tags;
}
