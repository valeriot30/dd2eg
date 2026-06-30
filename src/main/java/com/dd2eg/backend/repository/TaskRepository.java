package com.dd2eg.backend.repository;

import com.dd2eg.backend.DTO.FundedProjectSkillsDTO;
import com.dd2eg.backend.model.Task;
import com.dd2eg.backend.utils.TaskStatus;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {
    List<Task> findByProjectId(String projectId);
    List<Task> findByProjectIdAndStatus(String projectId, TaskStatus status);

    @Aggregation(pipeline = {
            "{ $match: { 'sponsorships.enterpriseId': ?0 } }",
            "{ $group: { _id: '$projectId', skillSets: { $push: { $ifNull: ['$skills', []] } } } }",
            """
            {
              $project: {
                _id: 0,
                projectId: '$_id',
                requiredSkills: {
                  $reduce: {
                    input: '$skillSets',
                    initialValue: [],
                    in: { $setUnion: ['$$value', '$$this'] }
                  }
                }
              }
            }
            """
    })
    List<FundedProjectSkillsDTO> findFundedProjectSkillsByEnterprise(String enterpriseId);
}
