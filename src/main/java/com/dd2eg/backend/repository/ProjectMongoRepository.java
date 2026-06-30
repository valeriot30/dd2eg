package com.dd2eg.backend.repository;

import com.dd2eg.backend.DTO.ProjectDTO;
import com.dd2eg.backend.model.Project;
import com.dd2eg.backend.DTO.TopContributorDTO;
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

    boolean existsByName(String name);

    @Aggregation(pipeline = {

            """
            { $match: { $expr: { $eq: [ { $toString: '$_id' }, ?0 ] } } }
            """,

            """
            { $addFields: { stringId: { $toString: '$_id' } } }
            """,

            """
            { $lookup: { 
                from: 'tasks', 
                localField: 'stringId', 
                foreignField: 'projectId', 
                as: 'tasks' 
            } }
            """,

            """
            { $project: {
                id: '$_id',
                name: '$name',
                description: '$description',
                ownerName: '$owner',
                openTasks: {
                    $filter: {
                        input: '$tasks',
                        as: 'task',
                        cond: { $eq: ['$$task.status', 'OPEN'] }
                    }
                },
                avgContributionsPerTask: {
                    $avg: {
                        $map: {
                            input: '$tasks',
                            as: 't',
                            in: { $size: { $ifNull: ['$$t.commits', []] } }
                        }
                    }
                },
                totalActiveContributors: {
                    $size: {
                        $reduce: {
                            input: '$tasks.contributors',
                            initialValue: [],
                            in: { $setUnion: ['$$value', '$$this'] }
                        }
                    }
                }
            } }
            """
    })
    ProjectDTO findProjectDetailsById(String projectId);
}
