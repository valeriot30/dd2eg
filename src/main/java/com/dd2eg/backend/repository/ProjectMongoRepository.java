package com.dd2eg.backend.repository;

import com.dd2eg.backend.DTO.ProjectDTO;
import com.dd2eg.backend.DTO.TopContributorDTO;
import com.dd2eg.backend.DTO.FundedProjectDTO;
import com.dd2eg.backend.model.Project;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProjectMongoRepository extends MongoRepository<Project, String> {
    List<Project> findAllBy(TextCriteria textCriteria);
    List<Project> findByInterestAreasIn(List<String> interestAreas);
    @Aggregation(pipeline = {
            "{ $match: { 'contributors.0': { $exists: true } } }",
            "{ $unwind: '$contributors' }",
            "{ $group: { _id: '$contributors', projectCount: { $sum: 1 } } }",
            "{ $project: { username: '$_id', projectCount: 1, _id: 0 } }",
            "{ $sort: { projectCount: -1 } }",
            "{ $limit: ?0 }"
    })
    List<TopContributorDTO> findTopContributors(int limit);

    boolean existsByName(String name);

    @Aggregation(pipeline = {
            "{ $match: { 'tasks.sponsorships.enterpriseId': ?0 } }",
            "{ $project: { name: 1, description: 1, fundedTasks: { $filter: { input: '$tasks', as: 'task', cond: { $in: [ ?0, '$$task.sponsorships.enterpriseId' ] } } } } }",
            "{ $project: { name: 1, description: 1, requiredSkills: { $reduce: { input: '$fundedTasks.skills', initialValue: [], in: { $setUnion: [ '$$value', '$$this' ] } } } } }"
    })
    List<FundedProjectDTO> findProjectsFundedByEnterprise(String enterpriseId);

}
