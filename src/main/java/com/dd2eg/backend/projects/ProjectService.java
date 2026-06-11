package com.dd2eg.backend.projects;

import com.dd2eg.backend.users.User;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    /**
     *  Add a user to the contribution list of a project
     * @param projectId
     * @param currentUser
     * @return
     */
    public Project addContributorToProject(String projectId, User currentUser) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        if (project.getStatus() != ProjectStatus.OPEN) {
            throw new RuntimeException("Cannot join a CLOSED project");
        }

        String username = currentUser.getUsername();

        if (project.getContributors().contains(username)) {
            throw new RuntimeException("User is already a contributor");
        }

        project.getContributors().add(username);

        return projectRepository.save(project);
    }

    /**
     * Remove a user from contribuition
     * @param projectId
     * @param currentUser
     * @return
     */
    public Project removeContributorFromProject(String projectId, User currentUser) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        String username = currentUser.getUsername();

        if (!project.getContributors().contains(username)) {
            throw new RuntimeException("User is not contributor of this project");
        }

        project.getContributors().remove(username);

        return projectRepository.save(project);
    }
}
