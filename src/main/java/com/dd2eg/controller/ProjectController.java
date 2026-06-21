package com.dd2eg.controller;

import com.dd2eg.model.Project;
import com.dd2eg.service.ProjectService;
import com.dd2eg.DTO.CreateProjectDTO;
import com.dd2eg.model.User;
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

    @PostMapping("/{projectId}/leave")
    @Operation(
            summary = "Leave a project",
            description = "Removes the authenticated user from project contributors"
    )
    public ResponseEntity<?> leaveProject(
            @PathVariable String projectId,
            @AuthenticationPrincipal User currentUser) {

        try {

            Project updatedProject = projectService.removeContributorFromProject(projectId, currentUser);

            return ResponseEntity.ok(updatedProject);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
     * Filter projects by tags
     * URL: GET /api/projects/filter?tags=react,spring,mongodb
     * @param tags the tags provided by the frontend
     * @return a list of projects matching the tags
     */
    @GetMapping("/filter")
    @Operation(
            summary = "Filter projects by tags",
            description = "Returns projects that match one or more tags"
    )
    public ResponseEntity<List<Project>> filterProjects(
            @RequestParam(name = "tags", required = false) List<String> tags) {

        List<Project> filteredProjects = projectService.filterProjectsByTags(tags);

        return ResponseEntity.ok(filteredProjects);
    }

}
