package com.dd2eg.backend.users;

import com.dd2eg.backend.tasks.Task;
import com.dd2eg.backend.tasks.events.Event;
import com.dd2eg.backend.tasks.events.EventRepository;
import com.dd2eg.backend.tasks.events.EventType;
import com.dd2eg.backend.users.dto.EnterpriseStatsDTO;
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

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class UserService {


    @Autowired
    PasswordEncoder passwordEncoder;

    private final UserMongoRepository userRepository;
    private final EventRepository eventRepository;
    private final MongoTemplate mongoTemplate;

    private User getUserById(String id) {
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
     * @param the created user
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
        document.put("skills", user.getSkills());
        event.setPayload(document.toJson());

        eventRepository.save(event);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser).getBody();
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

    public EnterpriseStatsDTO getDashboardStats(String enterpriseId) {

        //TODO CHECK IF USER IS ENTERPRISE, OTHERWISE RETURN USER DASHBOARD
        return this.getEnterpriseDashboardStats(enterpriseId);
    }


}
