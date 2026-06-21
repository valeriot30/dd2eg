package com.dd2eg.backend.repository;

import com.dd2eg.backend.model.Commit;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommitMongoRepository extends MongoRepository<Commit, String> {
}
