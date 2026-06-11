package com.dd2eg.backend.users;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserMongoRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
}
