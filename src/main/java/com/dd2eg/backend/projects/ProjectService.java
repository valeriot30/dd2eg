package com.dd2eg.backend.projects;

import com.dd2eg.backend.projects.dto.ProjectStatusDTO;
import com.dd2eg.backend.users.User;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Service
public class ProjectService {

    private ProjectMongoRepository projectRepository;

    private final MongoTemplate mongoTemplate;

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
    /**
     * Search projects by name using MongoDB Text Search
     * @param keyword the search query
     * @return list of projects matching the keyword
     */
    public List<Project> searchProjects(String keyword) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return projectRepository.findAll();
        }

        TextCriteria criteria = TextCriteria.forDefaultLanguage()
                .matchingAny(keyword.trim());

        return projectRepository.findAllBy(criteria);
    }

    /**
     * Get total funding budget for OPEN projects
     * @return the list of stats
     */
    public List<ProjectStatusDTO> getProjectStats() {

        Aggregation aggregation = Aggregation.newAggregation(

                Aggregation.match(Criteria.where("budget").gt(ProjectStatus.OPEN)),

                Aggregation.group("status")
                        .count().as("totalProjects")
                        .sum("budget").as("totalBudget"),

                Aggregation.sort(Sort.Direction.DESC, "totalProjects")
        );

        AggregationResults<ProjectStatusDTO> results = mongoTemplate.aggregate(
                aggregation,
                "projects",
                ProjectStatusDTO.class
        );

        return results.getMappedResults();
    }

    /**
     * Filter projects by a list of tags
     * @param tags list of tags to filter by
     * @return list of matching projects
     */
    public List<Project> filterProjectsByTags(List<String> tags) {

        if (tags == null || tags.isEmpty()) {
            return projectRepository.findAll();
        }

        return projectRepository.findByTagsIn(tags);
    }
}
