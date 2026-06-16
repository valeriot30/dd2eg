package com.dd2eg.backend.tasks;

import com.dd2eg.backend.tasks.dto.CreateCommentDTO;
import com.dd2eg.backend.tasks.dto.CreateTaskDTO;
import com.dd2eg.backend.tasks.dto.FundTaskRequestDTO;
import com.dd2eg.backend.users.User;
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
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/{taskId}/fund")
    public ResponseEntity<Task> fundTask(
            @PathVariable String taskId,
            @RequestBody FundTaskRequestDTO request,
            @AuthenticationPrincipal User currentUser) {

        Task updatedTask = taskService.fundTask(taskId, request, currentUser);

        return ResponseEntity.ok(updatedTask);
    }

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

    @GetMapping("/{projectId}")
    public List<Task> getTasks(@PathVariable String projectId) {
        return taskService.getTasksByProjectId(projectId);
    }

    @PostMapping("/add")
    public Task createTask(@RequestBody CreateTaskDTO request) {
        return taskService.createTask(request);
    }

}
