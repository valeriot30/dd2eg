package com.dd2eg.backend.users;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserMongoRepository extends MongoRepository<User, String> {
    User findByEmail(String email);
}
