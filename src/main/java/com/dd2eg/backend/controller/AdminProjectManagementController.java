package com.dd2eg.backend.controller;

import com.dd2eg.backend.service.ProjectService;
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
@Tag(name = "Admin Project Management", description = "Admin API for project management")
@RequestMapping("/api/admin/projects")
public class AdminProjectManagementController {

    private final ProjectService projectService;

    @Operation(
            summary = "Delete a project",
            description = "Deletes a project and its associated tasks. Accessible only by administrators."
    )
    @ApiResponse(responseCode = "200", description = "Project deleted successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    @ApiResponse(responseCode = "404", description = "Project not found")
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<?> deleteProject(@PathVariable String projectId) {
        try {
            projectService.deleteProject(projectId);
            return ResponseEntity.ok("Project has been successfully deleted.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
