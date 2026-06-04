package com.dd2eg.backend.projects;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api")
public class ProjectController {

    final ProjectService projectService;

    @GetMapping("/projects")
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }

    @PostMapping("/projects")
    public Project createProject(@RequestBody Project project) {
        log.info("Creating new project: {}", project.getName());
        return projectService.createProject(project);
    }

}
