package com.dd2eg.backend.tasks;

import com.dd2eg.backend.projects.Project;
import com.dd2eg.backend.projects.ProjectMongoRepository;
import com.dd2eg.backend.tasks.comments.Comment;
import com.dd2eg.backend.tasks.commits.Commit;
import com.dd2eg.backend.tasks.commits.CommitMongoRepository;
import com.dd2eg.backend.tasks.dto.CreateCommitDTO;
import com.dd2eg.backend.tasks.dto.CreateTaskDTO;
import com.dd2eg.backend.tasks.dto.FundTaskRequestDTO;
import com.dd2eg.backend.tasks.events.Event;
import com.dd2eg.backend.tasks.events.EventRepository;
import com.dd2eg.backend.tasks.events.EventType;
import com.dd2eg.backend.tasks.sponsorship.SponsorshipDTO;
import com.dd2eg.backend.users.User;
import com.dd2eg.backend.users.UserMongoRepository;
import com.dd2eg.backend.users.UserType;
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

    @Transactional
    public Commit addCommitToTask(String taskId, CreateCommitDTO request, User author) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (task.getStatus() != TaskStatus.OPEN) {
            throw new RuntimeException("Cannot add commits to a task that is not OPEN");
        }

        int index = task.getCurrentIndex();
        if (task.getNumMaxCommits() != null && index >= task.getNumMaxCommits()) {
            throw new RuntimeException("Maximum number of commits reached for this task");
        }

        // Create and save commit
        Commit commit = new Commit();
        commit.setHash(request.getHash());
        commit.setComment(request.getComment());
        commit.setNumLines(request.getNumLines());
        commit.setTaskId(taskId);
        commit.setAuthorId(author.getId());

        task.getCommits().set(index, commit);

        task.setCurrentIndex(index + 1);

        Commit savedCommit = commitRepository.save(commit);


        // Calculate and update user rating
        if (task.getNumMaxCommits() != null && task.getNumMaxCommits() > 0) {
            Double currentRating = author.getRating();
            if (currentRating == null) {
                currentRating = 0.0;
            }
            author.setRating(Math.min(5.0, currentRating + (1.0 / task.getNumMaxCommits())));
            userRepository.save(author);
        }

        taskRepository.save(task);

        return savedCommit;
    }

    public List<Task> getTasksByProjectId(String projectId) {
        // get all tasks by project id
        return taskRepository.findByProjectId(projectId);
    }

    @Transactional
    public Task createTask(CreateTaskDTO request) {

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
}
