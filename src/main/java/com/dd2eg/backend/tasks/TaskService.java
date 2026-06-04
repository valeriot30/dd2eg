package com.dd2eg.backend.tasks;

import com.dd2eg.backend.projects.Project;
import com.dd2eg.backend.projects.ProjectMongoRepository;
import com.dd2eg.backend.tasks.dto.CreateTaskDTO;
import com.dd2eg.backend.tasks.dto.FundTaskRequestDTO;
import com.dd2eg.backend.users.User;
import com.dd2eg.backend.users.UserMongoRepository;
import com.dd2eg.backend.users.UserType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserMongoRepository userRepository;
    private final ProjectMongoRepository projectRepository;

    public Task fundTask(String taskId, FundTaskRequestDTO request) {

        User enterprise = userRepository.findById(request.getEnterpriseUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (enterprise.getUserType() != UserType.ENTERPRISE) {
            throw new RuntimeException("Only enterprises can fund tasks");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (task.getSponsorships() == null) {
            task.setSponsorships(new ArrayList<>());
        }

        return taskRepository.save(task);
    }

    public List<Task> getTasksByProjectId(String projectId) {
        // get all tasks by project id
        return taskRepository.findByProjectId(projectId);
    }

    public Task createTask(CreateTaskDTO request) {

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setBody(request.getBody());

        task.setPriority(request.getPriority());
        task.setNumMaxCommits(request.getNumMaxCommits());

        task.setProjectId(project.getId());

        task.setStatus(TaskStatus.PENDING);

        task.setSponsorships(new ArrayList<>());

        return taskRepository.save(task);
    }
}
