package com.dd2eg.backend.projects;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProjectMongoRepository extends MongoRepository<Project, String> {
}
