package com.dd2eg.backend.controller;

import com.dd2eg.backend.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@Tag(name = "Admin Task Management", description = "Admin API for task management")
@RequestMapping("/api/admin/tasks")
public class AdminTaskManagementController {

    private final TaskService taskService;

    @Operation(
            summary = "Delete a task",
            description = "Deletes a task. Accessible only by administrators."
    )
    @ApiResponse(responseCode = "200", description = "Task deleted successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable String taskId) {
        try {
            taskService.deleteTask(taskId);
            return ResponseEntity.ok("Task has been successfully deleted.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
