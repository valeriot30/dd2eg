package com.dd2eg.backend.controller;

import com.dd2eg.backend.DTO.CreateProjectScamReportDTO;
import com.dd2eg.backend.DTO.CreateProjectDTO;
import com.dd2eg.backend.DTO.ProjectDTO;
import com.dd2eg.backend.model.Project;
import com.dd2eg.backend.model.User;
import com.dd2eg.backend.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@AllArgsConstructor
@RestController
@Tag(name = "Projects", description = "Projects management API")
@RequestMapping("/api/projects")
public class ProjectController {

    final ProjectService projectService;

    @Operation(summary = "Get all projects", description = "Returns a list of all projects in the system")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of projects")
    @GetMapping
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }

    @Operation(
            summary = "Create a new project",
            description = "Creates a new project for the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Project created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PostMapping
    public Project createProject(@RequestBody CreateProjectDTO project, @AuthenticationPrincipal User currentUser) {
        log.info("Creating new project: {}", project.getName());
        return projectService.createProject(project, currentUser);
    }

    @Operation(
            summary = "Report a project",
            description = "Creates a scam report for a project from the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Report created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or project")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PostMapping("/{projectId}/reports")
    public ResponseEntity<?> createProjectScamReport(
            @PathVariable String projectId,
            @RequestBody CreateProjectScamReportDTO request,
            @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(projectService.createProjectScamReport(projectId, request, currentUser));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Get project by ID",
            description = "Returns a single project based on its unique ID"
    )
    @ApiResponse(responseCode = "200", description = "Project found successfully")
    @ApiResponse(responseCode = "404", description = "Project not found")
    @GetMapping("/{projectId}")
    public ResponseEntity<?> getProjectById(@PathVariable String projectId) {
        try {
            ProjectDTO project = projectService.getProjectById(projectId);
            return ResponseEntity.ok(project);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search projects",
            description = "Search projects by keyword query (name, description, etc.)"
    )
    public ResponseEntity<List<Project>> searchProjects(
            @RequestParam(name = "q", required = false) String query) {

        List<Project> searchResults = projectService.searchProjects(query);

        return ResponseEntity.ok(searchResults);
    }

    /**
     * Filter projects by interest areas
     * URL: GET /api/projects/filter?interestAreas=react,spring,mongodb
     * @param interestAreas the interest areas provided by the frontend
     * @return a list of projects matching the interest areas
     */
    @GetMapping("/filter")
    @Operation(
            summary = "Filter projects by interest areas",
            description = "Returns projects that match one or more interest areas"
    )
    public ResponseEntity<List<Project>> filterProjects(
            @RequestParam(name = "interestAreas", required = false) List<String> interestAreas) {

        List<Project> filteredProjects = projectService.filterProjectsByInterestAreas(interestAreas);

        return ResponseEntity.ok(filteredProjects);
    }

}
