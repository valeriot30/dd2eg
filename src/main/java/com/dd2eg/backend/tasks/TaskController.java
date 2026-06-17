package com.dd2eg.backend.tasks;

import com.dd2eg.backend.tasks.commits.Commit;
import com.dd2eg.backend.tasks.dto.CreateCommitDTO;
import com.dd2eg.backend.tasks.dto.CreateCommentDTO;
import com.dd2eg.backend.tasks.dto.CreateTaskDTO;
import com.dd2eg.backend.tasks.dto.FundTaskRequestDTO;
import com.dd2eg.backend.users.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@AllArgsConstructor
@RestController
@Tag(name = "Tasks", description = "Tasks management API")
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;


    @Operation(
            summary = "Fund a task",
            description = "Allows an enterprise to fund a specific task"
    )
    @ApiResponse(responseCode = "200", description = "Task funded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or business rule violation")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PostMapping("/{taskId}/fund")
    public ResponseEntity<Task> fundTask(
            @PathVariable String taskId,
            @RequestBody FundTaskRequestDTO request,
            @AuthenticationPrincipal User currentUser) {

        Task updatedTask = taskService.fundTask(taskId, request, currentUser);

        return ResponseEntity.ok(updatedTask);
    }

    @Operation(
            summary = "Add comment to task",
            description = "Adds a comment to a specific task"
    )
    @ApiResponse(responseCode = "200", description = "Comment added successfully")
    @ApiResponse(responseCode = "400", description = "Invalid task or comment data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PostMapping("/{taskId}/comments/add")
    public ResponseEntity<?> addComment(
            @PathVariable String taskId,
            @Valid @RequestBody CreateCommentDTO dto,
            @AuthenticationPrincipal User currentUser) {
        try {
            Task updatedTask = taskService.addCommentToTask(taskId, dto.getContent(), currentUser);

            return ResponseEntity.ok(updatedTask);

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Add commit to task",
            description = "Adds a commit to a specific task and updates user rating"
    )
    @ApiResponse(responseCode = "200", description = "Commit added successfully")
    @ApiResponse(responseCode = "400", description = "Invalid task or commit data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PostMapping("/{taskId}/commits/add")
    public ResponseEntity<?> addCommit(
            @PathVariable String taskId,
            @Valid @RequestBody CreateCommitDTO dto,
            @AuthenticationPrincipal User currentUser) {
        try {
            Commit commit = taskService.addCommitToTask(taskId, dto, currentUser);
            return ResponseEntity.ok(commit);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Get tasks by project",
            description = "Returns all tasks associated with a given project ID"
    )
    @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully")
    @GetMapping("/{projectId}")
    public List<Task> getTasks(@PathVariable String projectId) {
        return taskService.getTasksByProjectId(projectId);
    }

    @Operation(
            summary = "Create a new task",
            description = "Creates a new task inside a project"
    )
    @ApiResponse(responseCode = "200", description = "Task created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid task data")
    @PostMapping("/add")
    public Task createTask(@RequestBody CreateTaskDTO request) {
        return taskService.createTask(request);
    }

}
