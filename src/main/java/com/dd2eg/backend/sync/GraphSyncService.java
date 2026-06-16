package com.dd2eg.backend.sync;

import com.dd2eg.backend.tasks.events.Event;
import com.dd2eg.backend.tasks.events.EventRepository;
import com.dd2eg.backend.tasks.events.EventStatus;
import com.dd2eg.backend.tasks.events.EventType;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servizio schedulato per la sincronizzazione MongoDB → Neo4j.
 *
 * Implementa il pattern Transactional Outbox:
 * 1. Legge gli Event con status PENDING dalla collezione MongoDB
 * 2. Per ogni evento, esegue la query Cypher corrispondente su Neo4j
 * 3. Se successo → status = COMPLETED
 * 4. Se errore → incrementa retryCount; se retryCount >= MAX_RETRIES → status = FAILED
 *
 * Gli eventi FAILED possono essere riprocessati manualmente dall'admin
 * tramite l'AdminSyncController.
 */
@Service
public class GraphSyncService {

    private static final Logger log = LoggerFactory.getLogger(GraphSyncService.class);

    @Value("${graph.sync.max-retries:5}")
    private int maxRetries;

    private final EventRepository eventRepository;
    private final Neo4jWriteRepository neo4jWriteRepository;

    public GraphSyncService(EventRepository eventRepository, Neo4jWriteRepository neo4jWriteRepository) {
        this.eventRepository = eventRepository;
        this.neo4jWriteRepository = neo4jWriteRepository;
    }

    /**
     * Job schedulato: processa tutti gli eventi PENDING.
     * L'intervallo è configurabile tramite la proprietà graph.sync.interval (default: 30s).
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
     * Riprocessa tutti gli eventi FAILED.
     * Chiamato dall'AdminSyncController per il resync manuale.
     *
     * return il numero di eventi riprocessati con successo
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
     * Riprocessa un singolo evento FAILED.
     *
     * eventId l'ID dell'evento da riprocessare
     * return true se il retry ha avuto successo
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
     * Processa un singolo evento invocando il metodo corretto
     * del Neo4jWriteRepository in base al tipo di evento.
     *
     * Il payload dell'evento è un JSON string salvato su MongoDB.
     */
    private void processEvent(Event event) {
        Document payload = Document.parse(event.getPayload());

        switch (event.getType()) {
            case ADD_USER -> processAddUser(payload);
            case ADD_PROJECT -> processAddProject(payload);
            case ADD_TASK -> processAddTask(payload);
            case FUNDING -> processFunding(payload);
            case ADD_CONTRIBUTOR_TO_PROJECT -> processAddContributor(payload);
            default -> throw new RuntimeException("Unknown event type: " + event.getType());
        }
    }

    private void processAddUser(Document payload) {
        String userId = payload.getString("userId");
        String userType = payload.getString("userType");

        // Estrai i nomi delle skill dal payload
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

        // ADMIN non va nel grafo
        if ("ADMIN".equalsIgnoreCase(userType)) {
            log.debug("[GraphSync] Skipping ADMIN user: {}", userId);
            return;
        }

        neo4jWriteRepository.createUser(userId, userType, skillNames);
    }

    private void processAddProject(Document payload) {
        String projectId = payload.getString("projectId");
        Object statusObj = payload.get("status");
        String status = statusObj != null ? statusObj.toString().toLowerCase() : "open";

        List<String> tags = payload.getList("tags", String.class);

        neo4jWriteRepository.createProject(projectId, status, tags);
    }

    private void processAddTask(Document payload) {
        String taskId = payload.getString("taskId");
        String projectId = payload.getString("projectId");
        List<String> skills = payload.getList("skills", String.class);

        neo4jWriteRepository.createTask(taskId, projectId, skills);
    }

    private void processFunding(Document payload) {
        String enterpriseId = payload.getString("enterpriseId");
        String taskId = payload.getString("taskId");

        neo4jWriteRepository.createFunding(enterpriseId, taskId);
    }

    private void processAddContributor(Document payload) {
        String projectId = payload.getString("projectId");
        String contributorId = payload.getString("contributorId");

        neo4jWriteRepository.addContributorToProject(contributorId, projectId);
    }
}
