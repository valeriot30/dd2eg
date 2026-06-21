package com.dd2eg.backend.service;

import com.dd2eg.backend.model.Project;
import com.dd2eg.backend.repository.ProjectMongoRepository;
import com.dd2eg.backend.model.Task;
import com.dd2eg.backend.repository.TaskRepository;
import com.dd2eg.backend.utils.TaskStatus;
import com.dd2eg.backend.model.Comment;
import com.dd2eg.backend.model.Commit;
import com.dd2eg.backend.repository.CommitMongoRepository;
import com.dd2eg.backend.DTO.CreateCommitDTO;
import com.dd2eg.backend.DTO.CreateTaskDTO;
import com.dd2eg.backend.DTO.FundTaskRequestDTO;
import com.dd2eg.backend.model.Event;
import com.dd2eg.backend.repository.EventRepository;
import com.dd2eg.backend.utils.EventType;
import com.dd2eg.backend.DTO.SponsorshipDTO;
import com.dd2eg.backend.model.User;
import com.dd2eg.backend.repository.UserMongoRepository;
import com.dd2eg.backend.utils.UserType;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final UserMongoRepository userRepository;
    private final ProjectMongoRepository projectRepository;
    private final CommitMongoRepository commitRepository;
    private final ProjectService projectService;

    @Transactional
    public Task fundTask(String taskId, FundTaskRequestDTO request, User enterprise) {

        if (enterprise == null) {
            throw new RuntimeException("Enterprise is null");
        }

        if (enterprise.getUserType() != UserType.ENTERPRISE) {
            throw new RuntimeException("Only enterprises can fund tasks");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (task.getSponsorships() == null) {
            task.setSponsorships(new ArrayList<>());
        }

        int amountToFund = request.getAmount();
        if (amountToFund <= 0) {
            throw new IllegalArgumentException("Funding amount must be greater than zero");
        }

        SponsorshipDTO sponsorship = new SponsorshipDTO();
        sponsorship.setEnterpriseId(enterprise.getId());
        sponsorship.setName(enterprise.getUsername());
        sponsorship.setAmount(amountToFund);

        task.getSponsorships().add(sponsorship);

        Event event = new Event();
        event.setType(EventType.FUNDING);

        Document document = new Document();
        document.put("taskId", task.getId());
        document.put("enterpriseId", enterprise.getId());
        event.setPayload(document.toJson());

        eventRepository.save(event);

        return taskRepository.save(task);
    }

    public Task addCommentToTask(String taskId, String content, User author) {

        // TODO check if task is open
        // mongodb index for open tasks can be used to speed-up the look-up

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task non found"));

        Comment newComment = new Comment();
        newComment.setContent(content);
        newComment.setAuthorId(author.getId());

        task.getComments().add(newComment);

        return taskRepository.save(task);
    }

    public Commit addCommitToTask(String taskId, CreateCommitDTO dto, User currentUser) {
        if (currentUser == null) {
            throw new RuntimeException("Authenticated user is required to commit on a task");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (task.getProjectId() == null || task.getProjectId().isBlank()) {
            throw new RuntimeException("Task does not belong to a project");
        }

        int index = task.getCurrentIndex();
        if (index >= task.getNumMaxCommits()) {
            throw new RuntimeException("Max commits reached for this task");
        }

        Commit commit = task.getCommits().get(index);
        commit.setHash(dto.getHash());
        commit.setComment(dto.getComment());
        commit.setNumLines(dto.getNumLines());

        commit.setTaskId(task.getId());
        commit.setProjectId(task.getProjectId());
        commit.setAuthorId(currentUser.getId());
        commit.setAuthorUsername(currentUser.getUsername());

        task.getCommits().set(index, commit);

        Commit savedCommit = commitRepository.save(commit);

        projectService.addContributorToProjectIfMissing(task.getProjectId(), currentUser);

        Event event = new Event();
        event.setType(EventType.ADD_WORKER_TO_TASK);

        Document document = new Document();
        document.put("taskId", task.getId());
        document.put("workerId", currentUser.getId());
        event.setPayload(document.toJson());

        eventRepository.save(event);

        taskRepository.save(task);

        return savedCommit;
    }

    public List<Task> getTasksByProjectId(String projectId) {
        // get all tasks by project id
        return taskRepository.findByProjectId(projectId);
    }

    @Transactional
    public Task createTask(CreateTaskDTO request, User user) {

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Task task = new Task(request.getNumMaxCommits());
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setBody(request.getBody());

        task.setPriority(request.getPriority());
        task.setNumMaxCommits(request.getNumMaxCommits());
        task.setSkills(request.getSkills());

        task.setProjectId(project.getId());

        //TODO CHANGE THIS TO PENDING
        task.setStatus(TaskStatus.OPEN);

        return taskRepository.save(task);
    }

    @Transactional
    public Task acceptTask(String taskId, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (task.getStatus() != TaskStatus.PENDING) {
            throw new RuntimeException("Task is not in PENDING state");
        }

        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (project.getCreatorId() == null || !currentUser.getId().equals(project.getCreatorId())) {
            throw new RuntimeException("Only the project creator can accept tasks");
        }

        task.setStatus(TaskStatus.OPEN);

        Event event = new Event();
        event.setType(EventType.ADD_TASK);

        Document document = new Document();
        document.put("taskId", task.getId());
        document.put("projectId", project.getId());
        document.put("acceptedBy", currentUser.getId());
        document.put("skills", task.getSkills());
        event.setPayload(document.toJson());

        eventRepository.save(event);

        return taskRepository.save(task);
    }

    /**
     * Get a task by id
     * @param taskId
     * @return
     */
    public Task getTaskById(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
    }
}
