package com.dd2eg.repository;

import com.dd2eg.model.Project;
import com.dd2eg.DTO.TopContributorDTO;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProjectMongoRepository extends MongoRepository<Project, String> {
    List<Project> findAllBy(TextCriteria textCriteria);
    List<Project> findByTagsIn(List<String> tags);
    @Aggregation(pipeline = {
            "{ $unwind: '$contributors' }",
            "{ $group: { _id: '$contributors', projectCount: { $sum: 1 } } }",
            "{ $project: { username: '$_id', projectCount: 1, _id: 0 } }",
            "{ $sort: { projectCount: -1 } }",
            "{ $limit: ?0 }"
    })
    List<TopContributorDTO> findTopContributors(int limit);
}
