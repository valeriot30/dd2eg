package com.dd2eg.backend.service;

import com.dd2eg.backend.model.*;
import com.dd2eg.backend.repository.*;
import com.dd2eg.backend.DTO.CreateDevReportDTO;
import com.dd2eg.backend.DTO.ReportedDeveloperDTO;
import com.dd2eg.backend.DTO.ProjectRecommendationDTO;
import com.dd2eg.backend.DTO.SkillRecommendationDTO;
import com.dd2eg.backend.utils.EventType;
import com.dd2eg.backend.DTO.DeveloperStatsDTO;
import com.dd2eg.backend.DTO.EnterpriseStatsDTO;
import com.dd2eg.backend.DTO.RecentProjectDTO;
import com.dd2eg.backend.utils.UserType;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class UserService {


    @Autowired
    PasswordEncoder passwordEncoder;

    private final UserMongoRepository userRepository;
    private final EventRepository eventRepository;
    private final SkillRepository skillRepository;
    private final MongoTemplate mongoTemplate;
    private final ProjectMongoRepository projectRepo;
    private final Neo4jRecommendationRepository neo4jRepo;

    public User getUserById(String id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Create a user
     * @param user the created user
     * @return The created user
     */
    @Transactional
    public User createUser(@RequestBody User user) {

        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        User savedUser = userRepository.save(user);

        Event event = new Event();
        event.setType(EventType.ADD_USER);

        Document document = new Document();
        document.put("userId", user.getId());
        document.put("userType", user.getUserType() != null ? user.getUserType().name() : "DEVELOPER");
        
        List<String> skillNames = new java.util.ArrayList<>();
        if (user.getSkills() != null) {
            for (com.dd2eg.backend.model.Skill s : user.getSkills()) {
                if (s.getName() != null) {
                    skillNames.add(s.getName());
                }
            }
        }
        document.put("skills", skillNames);
        event.setPayload(document.toJson());

        eventRepository.save(event);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser).getBody();
    }

    public User updateUserSkills(String userId, List<String> newSkills) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        List<Skill> skills = new ArrayList<>();

        for (String skill : newSkills) {
            Skill newSkill = new Skill();
            newSkill.setName(skill);
            skillRepository.save(newSkill);
            skills.add(newSkill);
        }

        user.setSkills(skills);
        User savedUser = userRepository.save(user);

        Event event = new Event();
        event.setType(EventType.UPDATE_USER_SKILLS);

        Document document = new Document();
        document.put("userId", user.getId());
        
        List<String> skillNames = new java.util.ArrayList<>();
        for (Skill s : skills) {
            if (s.getName() != null) {
                skillNames.add(s.getName());
            }
        }
        document.put("skills", skillNames);
        event.setPayload(document.toJson());

        eventRepository.save(event);

        return savedUser;
    }

    public DeveloperStatsDTO getDeveloperStats(String userId) {

        DeveloperStatsDTO stats = new DeveloperStatsDTO();

        List<ProjectRecommendationDTO> recs = neo4jRepo.getProjectRecommendations(userId);

        Map<String, ProjectRecommendationDTO> recMap = recs.stream()
                .collect(Collectors.toMap(ProjectRecommendationDTO::getRecommendedProjectId, r -> r));

        List<Project> enrichedProjects = projectRepo.findAllById(recMap.keySet());

        List<RecentProjectDTO> dashboardProjects = enrichedProjects.stream()
                .map(p -> {
                    ProjectRecommendationDTO rec = recMap.get(p.getId());
                    return new RecentProjectDTO(
                            p.getId(),
                            p.getName(),
                            p.getContributors().size(),
                            0
                    );
                })
                .toList();

        stats.setTrendingProjects(dashboardProjects);

        stats.setTopContributors(projectRepo.findTopContributors(10));

        List<SkillRecommendationDTO> skillRecs = neo4jRepo.getSkillRecommendations(userId);

        List<SkillRecommendationDTO> personalSkillTrends = skillRecs.stream()
                .map(s -> new SkillRecommendationDTO(s.getRecommendedSkill(), s.getFrequency()))
                .collect(Collectors.toList());

        stats.setTrendingSkills(personalSkillTrends);

        return stats;
    }

    public EnterpriseStatsDTO getEnterpriseDashboardStats(String enterpriseId) {
        Aggregation aggregation = Aggregation.newAggregation(

                Aggregation.match(Criteria.where("enterpriseId").is(enterpriseId)),

                Aggregation.group()
                        .sum(ConditionalOperators.when(ComparisonOperators.Eq.valueOf("status").equalToValue("OPEN"))
                                .then(1).otherwise(0)).as("openedTasks")

                        .sum(ConditionalOperators.when(ComparisonOperators.Eq.valueOf("status").equalToValue("COMPLETED"))
                                .then(1).otherwise(0)).as("completedTasks")

                        .sum(ConditionalOperators.when(ComparisonOperators.Eq.valueOf("status").equalToValue("COMPLETED"))
                                .thenValueOf("$budget").otherwise(0)).as("totalBudgetSpent")
        );

        AggregationResults<EnterpriseStatsDTO> results = mongoTemplate.aggregate(
                aggregation,
                "tasks",
                EnterpriseStatsDTO.class
        );

        EnterpriseStatsDTO dashboardData = results.getUniqueMappedResult();
        if (dashboardData == null) {
            dashboardData = new EnterpriseStatsDTO();
        }

        Query query = new Query(Criteria.where("enterpriseId").is(enterpriseId));
        List<String> uniqueDevelopers = mongoTemplate.findDistinct(
                query,
                "contributors",
                Task.class,
                String.class
        );

        dashboardData.setUniqueDevelopersInvolved(uniqueDevelopers.size());

        return dashboardData;
    }

    //TODO MAKE this general, so take a user dto as input and update all the fields

    /**
     * Ban a user, this function can be used later to update other informations
     * @param userId
     * @param isEnabled
     */
    public void updateUserStatus(String userId, boolean isEnabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (user.getUserType() == UserType.ADMIN) {
            throw new RuntimeException("Cannot alter the status of an administrator");
        }

        user.setEnabled(isEnabled);
        userRepository.save(user);
    }

    public DevReport createDevReport(String developerId, CreateDevReportDTO request, User reportingEnterprise) {
        if (reportingEnterprise == null) {
            throw new RuntimeException("Authenticated enterprise is required");
        }

        if (reportingEnterprise.getUserType() != UserType.ENTERPRISE) {
            throw new RuntimeException("Only enterprises can report developers");
        }

        if (request == null || request.getComment() == null || request.getComment().isBlank()) {
            throw new RuntimeException("Report comment is required");
        }

        User developer = userRepository.findById(developerId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + developerId));

        if (developer.getUserType() != UserType.DEVELOPER) {
            throw new RuntimeException("Reports can only be created for developers");
        }

        if (developer.getDeveloperInfo() == null) {
            developer.setDeveloperInfo(new DeveloperInfo());
        }

        if (developer.getDeveloperInfo().getDevReports() == null) {
            developer.getDeveloperInfo().setDevReports(new ArrayList<>());
        }

        DevReport report = new DevReport();
        report.setReportingEnterpriseId(reportingEnterprise.getId());
        report.setReportingEnterpriseName(reportingEnterprise.getUsername());
        report.setReportingEnterpriseProfilePic(reportingEnterprise.getProfilePic());
        report.setComment(request.getComment());

        developer.getDeveloperInfo().getDevReports().add(report);
        developer.getDeveloperInfo().setNumReports(developer.getDeveloperInfo().getDevReports().size());
        userRepository.save(developer);

        return report;
    }

    public List<ReportedDeveloperDTO> getReportedDevelopersAboveThreshold(int threshold) {
        return userRepository.findAll().stream()
                .filter(user -> user.getUserType() == UserType.DEVELOPER)
                .filter(user -> user.getDeveloperInfo() != null)
                .filter(user -> user.getDeveloperInfo().getNumReports() > threshold)
                .sorted((first, second) -> Integer.compare(
                        second.getDeveloperInfo().getNumReports(),
                        first.getDeveloperInfo().getNumReports()
                ))
                .map(user -> new ReportedDeveloperDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getProfilePic(),
                        user.getDeveloperInfo().getRating(),
                        user.getDeveloperInfo().getNumReports()
                ))
                .toList();
    }

    public List<DevReport> getDevReportsByDeveloperId(String developerId) {
        User developer = userRepository.findById(developerId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + developerId));

        if (developer.getUserType() != UserType.DEVELOPER) {
            throw new RuntimeException("Reports can only be listed for developers");
        }

        if (developer.getDeveloperInfo() == null || developer.getDeveloperInfo().getDevReports() == null) {
            return List.of();
        }

        return developer.getDeveloperInfo().getDevReports();
    }
}
