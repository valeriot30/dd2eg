package com.dd2eg.backend.projects;

import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProjectMongoRepository extends MongoRepository<Project, String> {
    List<Project> findAllBy(TextCriteria textCriteria);
    List<Project> findByTagsIn(List<String> tags);
}
