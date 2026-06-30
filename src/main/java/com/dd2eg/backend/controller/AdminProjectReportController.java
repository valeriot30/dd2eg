package com.dd2eg.backend.controller;

import com.dd2eg.backend.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@Tag(name = "Admin Project Reports", description = "Admin API for project scam reports")
@RequestMapping("/api/admin/projects")
public class AdminProjectReportController {

    private final ProjectService projectService;

    @Operation(
            summary = "Get reported projects",
            description = "Returns projects whose scam report count is above the requested threshold, ordered by report count descending"
    )
    @ApiResponse(responseCode = "200", description = "Reported projects retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid threshold")
    @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/reports")
    public ResponseEntity<?> getReportedProjects(@RequestParam int threshold) {
        if (threshold < 0) {
            return ResponseEntity.badRequest().body("Threshold must be greater than or equal to zero");
        }

        return ResponseEntity.ok(projectService.getReportedProjectsAboveThreshold(threshold));
    }

    @Operation(
            summary = "Get project scam reports",
            description = "Returns all scam reports created for the specified project"
    )
    @ApiResponse(responseCode = "200", description = "Project reports retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    @ApiResponse(responseCode = "404", description = "Project not found")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/{projectId}/reports")
    public ResponseEntity<?> getProjectScamReports(@PathVariable String projectId) {
        try {
            return ResponseEntity.ok(projectService.getProjectScamReports(projectId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
