package com.dd2eg.backend.repository;

import com.dd2eg.backend.model.Skill;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SkillRepository extends MongoRepository<Skill, String> {
}
