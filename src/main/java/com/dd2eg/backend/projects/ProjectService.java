package com.dd2eg.backend.projects;

import com.dd2eg.backend.projects.dto.CreateProjectDTO;
import com.dd2eg.backend.projects.dto.ProjectStatusDTO;
import com.dd2eg.backend.tasks.events.Event;
import com.dd2eg.backend.tasks.events.EventRepository;
import com.dd2eg.backend.tasks.events.EventType;
import com.dd2eg.backend.users.User;
import com.dd2eg.backend.users.dto.RecentProjectDTO;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class ProjectService {

    private ProjectMongoRepository projectRepository;
    private EventRepository eventRepository;
    private final MongoTemplate mongoTemplate;

    private static Integer NUM_LAST_PROJECTS = 10;

    /**
     * Retrieve all projects
     * 
     * @return the list of projects
     */
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    /**
     * Create a project
     * 
     * @param project
     * @return the created entity
     */
    @Transactional
    public Project createProject(CreateProjectDTO project, @AuthenticationPrincipal User currentUser) {

        Project newProject = new Project();

        newProject.setCreatedAt(java.time.Instant.now().toString());
        newProject.setUpdatedAt(java.time.Instant.now().toString());
        newProject.setName(project.getName());
        newProject.setCreatorId(currentUser.getId());
        newProject.setCreatorName(currentUser.getUsername());
        newProject.setDescription(project.getDescription());
        newProject.setTags(project.getTags());

        if (newProject.getStatus() == null) {
            newProject.setStatus(ProjectStatus.OPEN);
        }

        newProject.setCreatorId(currentUser.getId());

        Event event = new Event();
        event.setType(EventType.ADD_TASK);

        Document document = new Document();
        document.put("projectId", newProject.getId());
        document.put("status", newProject.getStatus().name());
        document.put("tags", project.getTags());
        event.setPayload(document.toJson());

        eventRepository.save(event);

        projectRepository.save(newProject);

        return newProject;
    }

    /**
     * Add a user to the contribution list of a project
     * 
     * @param projectId
     * @param currentUser
     * @return the project joined
     */
    @Transactional
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

        Event event = new Event();
        event.setType(EventType.ADD_CONTRIBUTOR_TO_PROJECT);

        Document document = new Document();
        document.put("projectId", project.getId());
        document.put("contributorId", currentUser.getId());
        event.setPayload(document.toJson());

        eventRepository.save(event);

        RecentProjectDTO recentProject = new RecentProjectDTO(
                project.getId(),
                project.getName(),
                project.getContributors().size(),
                project.getBudget() != null ? project.getBudget() : 0
        );

        Query userQuery = new Query(Criteria.where("id").is(currentUser.getId()));
        Update userUpdate = new Update().push("lastProjects")
                .atPosition(0)
                .slice(NUM_LAST_PROJECTS)
                        .each(recentProject);

        mongoTemplate.updateFirst(userQuery, userUpdate, User.class);

        return projectRepository.save(project);
    }

    /**
     * Remove a user from contribuition
     * 
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

        Event event = new Event();
        event.setType(EventType.REMOVE_CONTRIBUTOR_FROM_PROJECT);

        Document document = new Document();
        document.put("projectId", project.getId());
        document.put("contributorId", currentUser.getId());
        event.setPayload(document.toJson());

        eventRepository.save(event);

        Query userQuery = new Query(Criteria.where("id").is(currentUser.getId()));

        Update userUpdate = new Update().pull("lastProjects", new Document("projectId", projectId));

        mongoTemplate.updateFirst(userQuery, userUpdate, User.class);

        return projectRepository.save(project);
    }



    /**
     * Search projects by name using MongoDB Text Search
     * 
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
     * 
     * @return the list of stats
     */
    public List<ProjectStatusDTO> getProjectStats() {

        Aggregation aggregation = Aggregation.newAggregation(

                Aggregation.match(Criteria.where("budget").gt(ProjectStatus.OPEN)),

                Aggregation.group("status")
                        .count().as("totalProjects")
                        .sum("budget").as("totalBudget"),

                Aggregation.sort(Sort.Direction.DESC, "totalProjects"));

        AggregationResults<ProjectStatusDTO> results = mongoTemplate.aggregate(
                aggregation,
                "projects",
                ProjectStatusDTO.class);

        return results.getMappedResults();
    }

    /**
     * Filter projects by a list of tags
     * 
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
