package com.dd2eg.backend.service;

import com.dd2eg.backend.DTO.*;
import com.dd2eg.backend.model.Project;
import com.dd2eg.backend.model.ProjectScamReport;
import com.dd2eg.backend.repository.ProjectMongoRepository;
import com.dd2eg.backend.utils.ProjectStatus;
import com.dd2eg.backend.model.Event;
import com.dd2eg.backend.repository.EventRepository;
import com.dd2eg.backend.utils.EventType;
import com.dd2eg.backend.model.User;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@AllArgsConstructor
@Service
public class ProjectService {

    private ProjectMongoRepository projectRepository;
    private EventRepository eventRepository;
    private final MongoTemplate mongoTemplate;

    private static final Integer NUM_LAST_PROJECTS = 10;

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
        if (currentUser == null) {
            throw new RuntimeException("Authenticated user is required to create a project");
        }

        Project newProject = new Project();

        newProject.setCreatedAt(java.time.Instant.now().toString());
        newProject.setUpdatedAt(java.time.Instant.now().toString());
        newProject.setName(project.getName());
        newProject.setCreatorId(currentUser.getId());
        newProject.setDescription(project.getDescription());
        newProject.setTags(project.getTags());

        if (newProject.getStatus() == null) {
            newProject.setStatus(ProjectStatus.OPEN);
        }

        Project savedProject = projectRepository.save(newProject);

        OwnedProjectDTO projectSummary = new OwnedProjectDTO(
                savedProject.getId(),
                savedProject.getName(),
                savedProject.getDescription(),
                savedProject.getStatus()
        );

        Query userQuery = new Query(Criteria.where("id").is(currentUser.getId()));
        Update userUpdate = new Update().addToSet("ownedProjects", projectSummary);
        mongoTemplate.updateFirst(userQuery, userUpdate, User.class);
        newProject.setCreatorId(currentUser.getId());

        Event event = new Event();
        event.setType(EventType.ADD_PROJECT);

        Document document = new Document();
        document.put("projectId", savedProject.getId());
        document.put("creatorId", currentUser.getId());
        document.put("status", savedProject.getStatus().name());
        document.put("tags", project.getTags());
        event.setPayload(document.toJson());

        eventRepository.save(event);

        return savedProject;
    }

    public ProjectDTO getProjectById(String projectId) {
        return projectRepository.findProjectDetailsById(projectId);
    }

    /**
     * Add a user to the contribution list of a project
     *
     * @param projectId
     * @param currentUser
     * @return the project joined
     */
    @Transactional
    public Project addContributorToProjectIfMissing(String projectId, User currentUser) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        if (project.getStatus() != ProjectStatus.OPEN) {
            throw new RuntimeException("Cannot join a CLOSED project");
        }

        String userId = currentUser.getId();

        if (project.getContributors().contains(userId)) {
            addProjectToLastProjectsIfUserIsNotOwner(project, currentUser);
            return project;
        }


        project.getContributors().add(userId);

        Project savedProject = projectRepository.save(project);
        addProjectToLastProjectsIfUserIsNotOwner(savedProject, currentUser);

        return savedProject;
    }

    private void addProjectToLastProjectsIfUserIsNotOwner(Project project, User currentUser) {
        RecentProjectDTO recentProject = new RecentProjectDTO(
                project.getId(),
                project.getName(),
                project.getContributors().size(),
                project.getBudget() != null ? project.getBudget() : 0
        );

        Query userQuery = new Query(Criteria.where("id").is(currentUser.getId())
                .and("ownedProjects").not().elemMatch(Criteria.where("_id").is(project.getId()))
                .and("lastProjects").not().elemMatch(Criteria.where("projectId").is(project.getId())));
        Update userUpdate = new Update().push("lastProjects")
                .atPosition(0)
                .slice(NUM_LAST_PROJECTS)
                        .each(recentProject);

        mongoTemplate.updateFirst(userQuery, userUpdate, User.class);
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

        String userId = currentUser.getId();

        if (!project.getContributors().contains(userId)) {
            throw new RuntimeException("User is not contributor of this project");
        }

        project.getContributors().remove(userId);

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

    public ProjectScamReport createProjectScamReport(String projectId, CreateProjectScamReportDTO request, User reportingUser) {
        if (reportingUser == null) {
            throw new RuntimeException("Authenticated user is required to report a project");
        }

        if (request == null || request.getComment() == null || request.getComment().isBlank()) {
            throw new RuntimeException("Report comment is required");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        if (project.getScamReportList() == null) {
            project.setScamReportList(new ArrayList<>());
        }

        ProjectScamReport report = new ProjectScamReport();
        report.setReportingUserId(reportingUser.getId());
        report.setReportingUsername(reportingUser.getUsername());
        report.setReportingUserProfilePic(reportingUser.getProfilePic());
        report.setReportingUserType(reportingUser.getUserType());
        report.setComment(request.getComment());

        project.getScamReportList().add(report);
        project.setScamReports(project.getScamReportList().size());
        projectRepository.save(project);

        return report;
    }

    public List<ReportedProjectDTO> getReportedProjectsAboveThreshold(int threshold) {
        return projectRepository.findAll().stream()
                .filter(project -> getProjectReportCount(project) > threshold)
                .sorted((first, second) -> Integer.compare(
                        getProjectReportCount(second),
                        getProjectReportCount(first)
                ))
                .map(project -> new ReportedProjectDTO(
                        project.getId(),
                        project.getName(),
                        project.getDescription(),
                        getProjectReportCount(project)
                ))
                .toList();
    }

    public List<ProjectScamReport> getProjectScamReports(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        if (project.getScamReportList() == null) {
            return List.of();
        }

        return project.getScamReportList();
    }

    @Transactional
    public void deleteProject(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        Query taskQuery = new Query(Criteria.where("projectId").is(project.getId()));
        mongoTemplate.remove(taskQuery, "tasks");

        projectRepository.delete(project);
    }

    private int getProjectReportCount(Project project) {
        if (project.getScamReports() != null) {
            return project.getScamReports();
        }

        if (project.getScamReportList() == null) {
            return 0;
        }

        return project.getScamReportList().size();
    }


}
