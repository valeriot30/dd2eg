package com.dd2eg.backend.projects;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Document(collection = "projects")
public class Project {

    @Id
    private String id;

    private String name;

    private String description;

    private List<String> tags;

    private ProjectStatus status;

    @CreatedDate
    private String createdAt;

    @LastModifiedDate
    private String updatedAt;

    private Integer budget;

    private Integer scamReports;

    public List<String> contributors = new ArrayList<>(); // only names of contributors

}
