package com.dd2eg.backend.tasks.commits;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommitMongoRepository extends MongoRepository<Commit, String> {
}
