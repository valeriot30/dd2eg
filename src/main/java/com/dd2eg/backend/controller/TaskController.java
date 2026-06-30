package com.dd2eg.backend.controller;

import com.dd2eg.backend.model.Task;
import com.dd2eg.backend.service.TaskService;
import com.dd2eg.backend.model.Commit;
import com.dd2eg.backend.DTO.CreateCommitDTO;
import com.dd2eg.backend.DTO.CreateCommentDTO;
import com.dd2eg.backend.DTO.CreateTaskDTO;
import com.dd2eg.backend.DTO.FundTaskRequestDTO;
import com.dd2eg.backend.model.User;
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
    public Task createTask(@RequestBody CreateTaskDTO request, @AuthenticationPrincipal User currentUser) {
        return taskService.createTask(request, currentUser);
    }

    @Operation(
            summary = "Get task by ID",
            description = "Returns a single task based on its unique ID"
    )
    @ApiResponse(responseCode = "200", description = "Task found successfully")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @GetMapping("/detail/{taskId}")
    public ResponseEntity<?> getTaskById(@PathVariable String taskId) {
        try {
            Task task = taskService.getTaskById(taskId);
            return ResponseEntity.ok(task);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Accept a task",
            description = "Allows the project creator to accept a pending task and move it to OPEN status"
    )
    @ApiResponse(responseCode = "200", description = "Task accepted successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or business rule violation")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PutMapping("/{taskId}/accept")
    public ResponseEntity<?> acceptTask(
            @PathVariable String taskId,
            @AuthenticationPrincipal User currentUser) {
        try {
            Task updatedTask = taskService.acceptTask(taskId, currentUser);
            return ResponseEntity.ok(updatedTask);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Update a task",
            description = "Allows updating a task's properties such as skills"
    )
    @ApiResponse(responseCode = "200", description = "Task updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or business rule violation")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PutMapping("/{taskId}/update")
    public ResponseEntity<?> updateTask(
            @PathVariable String taskId,
            @RequestBody com.dd2eg.backend.DTO.UpdateTaskDTO dto,
            @AuthenticationPrincipal User currentUser) {
        try {
            Task updatedTask = taskService.updateTask(taskId, dto, currentUser);
            return ResponseEntity.ok(updatedTask);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
