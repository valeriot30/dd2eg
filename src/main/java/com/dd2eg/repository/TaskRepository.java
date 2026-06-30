package com.dd2eg.repository;

import com.dd2eg.model.Task;
import com.dd2eg.utils.TaskStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {
    List<Task> findByProjectId(String projectId);
    List<Task> findByProjectIdAndStatus(String projectId, TaskStatus status);
}
