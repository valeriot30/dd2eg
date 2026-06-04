package com.dd2eg.backend.tasks;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface TaskMongoRepository extends MongoRepository<Task, String> {
}
