package com.dd2eg.backend.neo4j.sync;

import com.dd2eg.backend.neo4j.Neo4jRecommendationRepository;
import com.dd2eg.backend.neo4j.AnomalyDetectionService;
import com.dd2eg.backend.neo4j.dto.AnomalyScanResultDTO;
import com.dd2eg.backend.neo4j.dto.CrossEnterpriseAnomalyDTO;
import com.dd2eg.backend.neo4j.dto.DeveloperEnterpriseAnomalyDTO;
import com.dd2eg.backend.tasks.events.Event;
import com.dd2eg.backend.tasks.events.EventRepository;
import com.dd2eg.backend.tasks.events.EventStatus;
import com.dd2eg.backend.tasks.events.EventType;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Scheduled service for MongoDB → Neo4j synchronization.
 *
 * Implements the Transactional Outbox pattern:
 * 1. Reads Events with PENDING status from the MongoDB collection
 * 2. For each event, executes the corresponding Cypher query on Neo4j
 * 3. On success → status = COMPLETED
 * 4. On error → increments retryCount; if retryCount >= MAX_RETRIES → status = FAILED
 *
 * FAILED events can be manually reprocessed by the admin
 * through the AdminSyncController.
 *
 * After a FUNDING event is synced, triggers anomaly detection
 * for the involved Enterprise and saves alerts to MongoDB.
 */
@Service
public class GraphSyncService {

    private static final Logger log = LoggerFactory.getLogger(GraphSyncService.class);

    @Value("${graph.sync.max-retries:5}")
    private int maxRetries;

    private final EventRepository eventRepository;
    private final Neo4jWriteRepository neo4jWriteRepository;
    private final AnomalyDetectionService anomalyDetectionService;
    private final MongoTemplate mongoTemplate;

    public GraphSyncService(EventRepository eventRepository,
                            Neo4jWriteRepository neo4jWriteRepository,
                            AnomalyDetectionService anomalyDetectionService,
                            MongoTemplate mongoTemplate) {
        this.eventRepository = eventRepository;
        this.neo4jWriteRepository = neo4jWriteRepository;
        this.anomalyDetectionService = anomalyDetectionService;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Scheduled job: processes all PENDING events.
     * The interval is configurable via the graph.sync.interval property (default: 30s).
     */
    @Scheduled(fixedRateString = "${graph.sync.interval:30000}")
    public void syncPendingEvents() {
        List<Event> pendingEvents = eventRepository.findByStatusOrderByCreatedAtAsc(EventStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("[GraphSync] Processing {} pending events...", pendingEvents.size());

        int successCount = 0;
        int failCount = 0;

        for (Event event : pendingEvents) {
            try {
                processEvent(event);
                event.setStatus(EventStatus.COMPLETED);
                event.setProcessedAt(LocalDateTime.now());
                event.setErrorMessage(null);
                successCount++;
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setErrorMessage(e.getMessage());

                if (event.getRetryCount() >= maxRetries) {
                    event.setStatus(EventStatus.FAILED);
                    log.error("[GraphSync] FAILED — Event {} (type={}) after {} retries: {}",
                            event.getId(), event.getType(), maxRetries, e.getMessage());
                    failCount++;
                } else {
                    log.warn("[GraphSync] RETRY {}/{} — Event {} (type={}): {}",
                            event.getRetryCount(), maxRetries,
                            event.getId(), event.getType(), e.getMessage());
                }
            }
            eventRepository.save(event);
        }

        log.info("[GraphSync] Completed: {} success, {} failed, {} still pending",
                successCount, failCount,
                pendingEvents.size() - successCount - failCount);
    }

    /**
     * Reprocesses all FAILED events.
     * Called by AdminSyncController for manual resync.
     *
     * @return the number of successfully reprocessed events
     */
    public int retryFailedEvents() {
        List<Event> failedEvents = eventRepository.findByStatus(EventStatus.FAILED);

        if (failedEvents.isEmpty()) {
            log.info("[GraphSync] No FAILED events to retry.");
            return 0;
        }

        log.info("[GraphSync] Admin retry: processing {} FAILED events...", failedEvents.size());

        int successCount = 0;
        for (Event event : failedEvents) {
            try {
                processEvent(event);
                event.setStatus(EventStatus.COMPLETED);
                event.setProcessedAt(LocalDateTime.now());
                event.setErrorMessage(null);
                successCount++;
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setErrorMessage("Admin retry failed: " + e.getMessage());
                log.error("[GraphSync] Admin retry FAILED for event {}: {}",
                        event.getId(), e.getMessage());
            }
            eventRepository.save(event);
        }

        log.info("[GraphSync] Admin retry completed: {}/{} success", successCount, failedEvents.size());
        return successCount;
    }

    /**
     * Reprocesses a single FAILED event.
     *
     * @param eventId the ID of the event to reprocess
     * @return true if the retry was successful
     */
    public boolean retrySingleEvent(String eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        if (event.getStatus() != EventStatus.FAILED) {
            throw new RuntimeException("Event " + eventId + " is not in FAILED status (current: " + event.getStatus() + ")");
        }

        log.info("[GraphSync] Admin retry for single event: {} (type={})", eventId, event.getType());

        try {
            processEvent(event);
            event.setStatus(EventStatus.COMPLETED);
            event.setProcessedAt(LocalDateTime.now());
            event.setErrorMessage(null);
            eventRepository.save(event);
            log.info("[GraphSync] Admin retry SUCCESS for event: {}", eventId);
            return true;
        } catch (Exception e) {
            event.setRetryCount(event.getRetryCount() + 1);
            event.setErrorMessage("Admin retry failed: " + e.getMessage());
            eventRepository.save(event);
            log.error("[GraphSync] Admin retry FAILED for event {}: {}", eventId, e.getMessage());
            return false;
        }
    }

    /**
     * Processes a single event by invoking the correct method
     * of Neo4jWriteRepository based on the event type.
     *
     * The event payload is a JSON string stored in MongoDB.
     */
    private void processEvent(Event event) {
        Document payload = Document.parse(event.getPayload());

        switch (event.getType()) {
            case ADD_USER -> processAddUser(payload);
            case ADD_PROJECT -> processAddProject(payload);
            case ADD_TASK -> processAddTask(payload);
            case FUNDING -> processFunding(payload);
            case ADD_WORKER_TO_TASK -> processAddWorkerToTask(payload);
            default -> throw new RuntimeException("Unknown event type: " + event.getType());
        }
    }

    private void processAddUser(Document payload) {
        String userId = payload.getString("userId");
        String userType = payload.getString("userType");

        // Extract skill names from the payload
        List<String> skillNames = new ArrayList<>();
        List<?> skills = payload.getList("skills", Object.class);
        if (skills != null) {
            for (Object skill : skills) {
                if (skill instanceof Document skillDoc) {
                    String name = skillDoc.getString("name");
                    if (name != null) {
                        skillNames.add(name);
                    }
                } else if (skill instanceof String skillName) {
                    skillNames.add(skillName);
                }
            }
        }

        // ADMIN users are not added to the graph
        if ("ADMIN".equalsIgnoreCase(userType)) {
            log.debug("[GraphSync] Skipping ADMIN user: {}", userId);
            return;
        }

        neo4jWriteRepository.createUser(userId, userType, skillNames);
    }

    private void processAddProject(Document payload) {
        String projectId = payload.getString("projectId");
        String creatorId = payload.getString("creatorId");
        Object statusObj = payload.get("status");
        String status = statusObj != null ? statusObj.toString().toLowerCase() : "open";

        List<String> tags = payload.getList("tags", String.class);

        neo4jWriteRepository.createProject(projectId, creatorId, status, tags);
    }

    private void processAddTask(Document payload) {
        String taskId = payload.getString("taskId");
        String projectId = payload.getString("projectId");
        List<String> skills = payload.getList("skills", String.class);

        neo4jWriteRepository.createTask(taskId, projectId, skills);
    }

    /**
     * Processes a FUNDING event:
     * 1. Creates the FINANCED relationship on Neo4j
     * 2. Runs anomaly detection for the involved Enterprise
     * 3. If suspicious cycles are found, saves alerts to the "anomaly_alerts" MongoDB collection
     */
    private void processFunding(Document payload) {
        String enterpriseId = payload.getString("enterpriseId");
        String taskId = payload.getString("taskId");

        // Step 1: Create the FINANCED relationship
        neo4jWriteRepository.createFunding(enterpriseId, taskId);

        // Step 2: Run anomaly detection for this specific enterprise
        try {
            AnomalyScanResultDTO anomalies = anomalyDetectionService.detectForEnterprise(enterpriseId);

            if (anomalies.getTotalAnomalies() > 0) {
                log.warn("[AnomalyDetection] Enterprise {} — Found {} anomalies after funding task {}",
                        enterpriseId, anomalies.getTotalAnomalies(), taskId);

                // Step 3: Save alerts to MongoDB
                if (anomalies.getCrossEnterpriseAnomalies() != null) {
                    for (CrossEnterpriseAnomalyDTO anomaly : anomalies.getCrossEnterpriseAnomalies()) {
                        Document alert = new Document();
                        alert.put("type", "CROSS_ENTERPRISE");
                        alert.put("enterpriseA", anomaly.getEnterpriseA());
                        alert.put("enterpriseB", anomaly.getEnterpriseB());
                        alert.put("tasksFinancedByAInB", anomaly.getTasksFinancedByAInB());
                        alert.put("tasksFinancedByBInA", anomaly.getTasksFinancedByBInA());
                        alert.put("triggerTaskId", taskId);
                        alert.put("detectedAt", LocalDateTime.now().toString());
                        alert.put("resolved", false);

                        mongoTemplate.save(alert, "anomaly_alerts");
                        log.warn("[AnomalyDetection]   → Cross-Enterprise match with Enterprise {}", anomaly.getEnterpriseB());
                    }
                }

                if (anomalies.getDeveloperEnterpriseAnomalies() != null) {
                    for (DeveloperEnterpriseAnomalyDTO anomaly : anomalies.getDeveloperEnterpriseAnomalies()) {
                        Document alert = new Document();
                        alert.put("type", "DEVELOPER_ENTERPRISE");
                        alert.put("complicitEnterpriseId", anomaly.getComplicitEnterpriseId());
                        alert.put("fraudsterDeveloperId", anomaly.getFraudsterDeveloperId());
                        alert.put("shellProjectId", anomaly.getShellProjectId());
                        alert.put("fakeTasksCompleted", anomaly.getFakeTasksCompleted());
                        alert.put("compromisedTaskIds", anomaly.getCompromisedTaskIds());
                        alert.put("triggerTaskId", taskId);
                        alert.put("detectedAt", LocalDateTime.now().toString());
                        alert.put("resolved", false);

                        mongoTemplate.save(alert, "anomaly_alerts");
                        log.warn("[AnomalyDetection]   → Dev-Enterprise match with Developer {}", anomaly.getFraudsterDeveloperId());
                    }
                }
            }
        } catch (Exception e) {
            // Anomaly detection failure should NOT block the sync
            log.error("[AnomalyDetection] Error detecting anomalies for enterprise {}: {}",
                    enterpriseId, e.getMessage());
        }
    }

    private void processAddWorkerToTask(Document payload) {
        String taskId = payload.getString("taskId");
        String workerId = payload.getString("workerId");

        neo4jWriteRepository.addWorkerToTask(workerId, taskId);
    }
}
