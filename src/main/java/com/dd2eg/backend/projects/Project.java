package com.dd2eg.backend.projects;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@Document(collection = "projects")
public class Project {

    @Id
    private String Id;

    private String name;

    private String description;

    private List<String> tags;

    private ProjectStatus status;

    private String createdAt;
    private String updatedAt;

    private Integer budget;

    public List<String> contributors; // only names of contributors

}
