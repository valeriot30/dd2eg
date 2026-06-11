package com.dd2eg.backend.projects;

import com.dd2eg.backend.users.User;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    final ProjectService projectService;

    @GetMapping("/")
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }

    @PostMapping("/")
    public Project createProject(@RequestBody Project project) {
        log.info("Creating new project: {}", project.getName());
        return projectService.createProject(project);
    }

    @PostMapping("/{projectId}/join")
    public ResponseEntity<?> joinProject(
            @PathVariable String projectId,
            @AuthenticationPrincipal User currentUser) {

        try {
            Project updatedProject = projectService.addContributorToProject(projectId, currentUser);
            return ResponseEntity.ok(updatedProject);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{projectId}/leave")
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
    public ResponseEntity<List<Project>> searchProjects(
            @RequestParam(name = "q", required = false) String query) {

        List<Project> searchResults = projectService.searchProjects(query);

        return ResponseEntity.ok(searchResults);
    }

}
