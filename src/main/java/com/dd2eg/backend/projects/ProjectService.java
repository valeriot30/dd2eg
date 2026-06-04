package com.dd2eg.backend.projects;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class ProjectService {

    private ProjectMongoRepository projectRepository;

    /**
     * Retrieve all projects
     * @return
     */
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    /**
     * Create a project
     * @param Project project
     * @return the created entity
     */
    public Project createProject(Project project) {
        project.setCreatedAt(java.time.Instant.now().toString());
        project.setUpdatedAt(java.time.Instant.now().toString());

        if (project.getStatus() == null) {
            project.setStatus(ProjectStatus.OPEN);
        }

        return projectRepository.save(project);
    }
}
