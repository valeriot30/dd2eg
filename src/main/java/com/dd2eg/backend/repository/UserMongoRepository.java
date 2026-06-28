package com.dd2eg.backend.repository;

import com.dd2eg.backend.model.User;
import com.dd2eg.backend.utils.UserType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserMongoRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);

    boolean existsByUserType(UserType userType);
}
