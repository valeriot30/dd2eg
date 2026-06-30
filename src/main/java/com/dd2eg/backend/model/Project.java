package com.dd2eg.backend.model;

import com.dd2eg.backend.utils.ProjectStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Sharded;
import org.springframework.data.mongodb.core.mapping.ShardingStrategy;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Document(collection = "projects")
@Sharded(shardKey = { "_id" }, shardingStrategy = ShardingStrategy.HASH)
public class Project {

    @Id
    private String id;

    @TextIndexed
    @Indexed(unique = true)
    private String name;

    private String description;

    @Indexed
    private List<String> interestAreas;

    private ProjectStatus status;

    @CreatedDate
    private String createdAt;

    @LastModifiedDate
    private String updatedAt;

    private Integer budget;

    private Integer scamReports;

    private List<ProjectScamReport> scamReportList = new ArrayList<>();

    private String creatorId;

    public List<String> contributors = new ArrayList<>(); // only ids of contributors

}
