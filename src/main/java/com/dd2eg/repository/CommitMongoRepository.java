package com.dd2eg.repository;

import com.dd2eg.model.Commit;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommitMongoRepository extends MongoRepository<Commit, String> {
}
