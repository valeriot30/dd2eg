package com.dd2eg.backend;

import com.dd2eg.backend.DTO.*;
import com.dd2eg.backend.model.*;
import com.dd2eg.backend.repository.*;
import com.dd2eg.backend.service.UserService;
import com.dd2eg.backend.utils.UserType;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMongoRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private ProjectMongoRepository projectRepo;

    @Mock
    private Neo4jRecommendationRepository neo4jRepo;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserById_ShouldReturnUser_WhenExists() {
        User user = new User();
        user.setId("1");
        when(userRepository.findById("1")).thenReturn(Optional.of(user));

        User result = userService.getUserById("1");

        assertNotNull(result);
        assertEquals("1", result.getId());
    }

    @Test
    void getUserById_ShouldReturnNull_WhenNotExists() {
        when(userRepository.findById("1")).thenReturn(Optional.empty());

        User result = userService.getUserById("1");

        assertNull(result);
    }

    @Test
    void getAllUsers_ShouldReturnList() {
        User u1 = new User();
        User u2 = new User();
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<User> result = userService.getAllUsers();

        assertEquals(2, result.size());
    }

    @Test
    void getUserByEmail_ShouldReturnOptionalUser_WhenExists() {
        User user = new User();
        user.setEmail("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.getUserByEmail("test@test.com");

        assertTrue(result.isPresent());
        assertEquals("test@test.com", result.get().getEmail());
    }

    @Test
    void createUser_ShouldEncodePasswordAndSaveUserAndFireEvent() {
        User user = new User();
        user.setId("userId123");
        user.setPassword("plainPassword");
        user.setUserType(UserType.DEVELOPER);
        Skill s1 = new Skill();
        s1.setName("Java");
        user.setSkills(List.of(s1));

        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(Event.class))).thenReturn(new Event());

        User result = userService.createUser(user);

        assertNotNull(result);
        assertEquals("encodedPassword", result.getPassword());
        verify(passwordEncoder).encode("plainPassword");
        verify(userRepository).save(user);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void updateUserSkills_ShouldSaveSkillsAndUserAndFireEvent() {
        User user = new User();
        user.setId("userId123");
        user.setSkills(new ArrayList<>());
        when(userRepository.findById("userId123")).thenReturn(Optional.of(user));
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(Event.class))).thenReturn(new Event());

        User result = userService.updateUserSkills("userId123", List.of("Java", "Spring"));

        assertNotNull(result);
        assertEquals(2, result.getSkills().size());
        assertEquals("Java", result.getSkills().get(0).getName());
        assertEquals("Spring", result.getSkills().get(1).getName());

        verify(skillRepository, times(2)).save(any(Skill.class));
        verify(userRepository).save(any(User.class));
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void updateUserSkills_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findById("userId123")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.updateUserSkills("userId123", List.of("Java")));
    }

    @Test
    void getDeveloperStats_ShouldReturnEnrichedStats() {
        String userId = "userId123";

        ProjectRecommendationDTO rec1 = new ProjectRecommendationDTO("proj1", 2, 1, List.of("Java"));
        when(neo4jRepo.getProjectRecommendations(userId)).thenReturn(List.of(rec1));

        Project project = new Project();
        project.setId("proj1");
        project.setName("Project One");
        project.setContributors(List.of("contrib1", "contrib2"));
        when(projectRepo.findAllById(anySet())).thenReturn(List.of(project));

        List<TopContributorDTO> topContributors = List.of(new TopContributorDTO("contrib1", 5));
        when(projectRepo.findTopContributors(10)).thenReturn(topContributors);

        SkillRecommendationDTO skillRec = new SkillRecommendationDTO("Spring", 3);
        when(neo4jRepo.getSkillRecommendations(userId)).thenReturn(List.of(skillRec));

        DeveloperStatsDTO stats = userService.getDeveloperStats(userId);

        assertNotNull(stats);
        assertEquals(1, stats.getTrendingProjects().size());
        assertEquals("Project One", stats.getTrendingProjects().get(0).getName());
        assertEquals(2, stats.getTrendingProjects().get(0).getNumContributors());
        assertEquals(1, stats.getTopContributors().size());
        assertEquals("contrib1", stats.getTopContributors().get(0).getUsername());
        assertEquals(1, stats.getTrendingSkills().size());
        assertEquals("Spring", stats.getTrendingSkills().get(0).getRecommendedSkill());
    }

    @Test
    void getEnterpriseDashboardStats_ShouldReturnAggregatedStats() {
        String enterpriseId = "ent123";

        EnterpriseStatsDTO aggregatedStats = new EnterpriseStatsDTO();
        aggregatedStats.setOpenedTasks(5);
        aggregatedStats.setCompletedTasks(10);
        aggregatedStats.setTotalBudgetSpent(1000);

        @SuppressWarnings("unchecked")
        AggregationResults<EnterpriseStatsDTO> aggregationResults = mock(AggregationResults.class);
        when(aggregationResults.getUniqueMappedResult()).thenReturn(aggregatedStats);

        when(mongoTemplate.aggregate(any(Aggregation.class), eq("tasks"), eq(EnterpriseStatsDTO.class)))
                .thenReturn(aggregationResults);

        List<String> devs = List.of("dev1", "dev2");
        when(mongoTemplate.findDistinct(any(Query.class), eq("commits.authorId"), eq(Task.class), eq(String.class)))
                .thenReturn(devs);

        @SuppressWarnings("unchecked")
        AggregationResults<Document> contributionResults = mock(AggregationResults.class);
        when(contributionResults.getUniqueMappedResult())
                .thenReturn(new Document("totalContributions", 3));
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("tasks"), eq(Document.class)))
                .thenReturn(contributionResults);

        EnterpriseStatsDTO result = userService.getEnterpriseDashboardStats(enterpriseId);

        assertNotNull(result);
        assertEquals(5, result.getOpenedTasks());
        assertEquals(10, result.getCompletedTasks());
        assertEquals(1000, result.getTotalBudgetSpent());
        assertEquals(3, result.getTotalContributions());
        assertEquals(2, result.getUniqueDevelopersInvolved());
    }

    @Test
    void getEnterpriseDashboardStats_ShouldReturnEmptyStats_WhenNoAggregationResult() {
        String enterpriseId = "ent123";

        @SuppressWarnings("unchecked")
        AggregationResults<EnterpriseStatsDTO> aggregationResults = mock(AggregationResults.class);
        when(aggregationResults.getUniqueMappedResult()).thenReturn(null);

        when(mongoTemplate.aggregate(any(Aggregation.class), eq("tasks"), eq(EnterpriseStatsDTO.class)))
                .thenReturn(aggregationResults);

        when(mongoTemplate.findDistinct(any(Query.class), eq("commits.authorId"), eq(Task.class), eq(String.class)))
                .thenReturn(List.of());

        @SuppressWarnings("unchecked")
        AggregationResults<Document> contributionResults = mock(AggregationResults.class);
        when(contributionResults.getUniqueMappedResult()).thenReturn(null);
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("tasks"), eq(Document.class)))
                .thenReturn(contributionResults);

        EnterpriseStatsDTO result = userService.getEnterpriseDashboardStats(enterpriseId);

        assertNotNull(result);
        assertEquals(0, result.getOpenedTasks());
        assertEquals(0, result.getCompletedTasks());
        assertEquals(0, result.getTotalBudgetSpent());
        assertEquals(0, result.getTotalContributions());
        assertEquals(0, result.getUniqueDevelopersInvolved());
    }
}
