package com.dd2eg.backend.sync;

import com.dd2eg.backend.tasks.events.Event;
import com.dd2eg.backend.tasks.events.EventRepository;
import com.dd2eg.backend.tasks.events.EventStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller REST per la gestione manuale della sincronizzazione Neo4j.
 * Accessibile solo agli amministratori.
 *
 * Permette di:
 * - Visualizzare gli eventi FAILED
 * - Ritentare il sync manualmente (singolo o batch)
 * - Consultare le statistiche di sync
 */
@RestController
@RequestMapping("/api/admin/sync")
@Tag(name = "Admin Sync", description = "API for managing MongoDB → Neo4j synchronization")
public class AdminSyncController {

    private final GraphSyncService graphSyncService;
    private final EventRepository eventRepository;

    public AdminSyncController(GraphSyncService graphSyncService, EventRepository eventRepository) {
        this.graphSyncService = graphSyncService;
        this.eventRepository = eventRepository;
    }

    /**
     * Riprocessa tutti gli eventi FAILED.
     * POST /api/admin/sync/retry-all
     */
    @Operation(
            summary = "Retry all failed events",
            description = "Reprocesses all events in FAILED status to resynchronize the Neo4j graph"
    )
    @ApiResponse(responseCode = "200", description = "Retry completed")
    @PostMapping("/retry-all")
    public ResponseEntity<Map<String, Object>> retryAllFailed() {
        int successCount = graphSyncService.retryFailedEvents();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Retry completed");
        response.put("successCount", successCount);

        return ResponseEntity.ok(response);
    }

    /**
     * Riprocessa un singolo evento FAILED.
     * POST /api/admin/sync/retry/{eventId}
     */
    @Operation(
            summary = "Retry a single failed event",
            description = "Reprocesses a single FAILED event specified by ID"
    )
    @ApiResponse(responseCode = "200", description = "Retry completed")
    @ApiResponse(responseCode = "400", description = "Event not found or not in FAILED status")
    @PostMapping("/retry/{eventId}")
    public ResponseEntity<Map<String, Object>> retrySingle(@PathVariable String eventId) {
        try {
            boolean success = graphSyncService.retrySingleEvent(eventId);

            Map<String, Object> response = new HashMap<>();
            response.put("eventId", eventId);
            response.put("success", success);
            response.put("message", success ? "Event synced successfully" : "Retry failed");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Lista tutti gli eventi FAILED.
     * GET /api/admin/sync/failed
     */
    @Operation(
            summary = "List all failed events",
            description = "Returns the list of all events that exceeded the maximum number of retries"
    )
    @ApiResponse(responseCode = "200", description = "List of FAILED events")
    @GetMapping("/failed")
    public ResponseEntity<List<Event>> getFailedEvents() {
        List<Event> failedEvents = eventRepository.findByStatus(EventStatus.FAILED);
        return ResponseEntity.ok(failedEvents);
    }

    /**
     * Statistiche di sincronizzazione.
     * GET /api/admin/sync/stats
     */
    @Operation(
            summary = "Get sync statistics",
            description = "Returns the event count grouped by status (PENDING, COMPLETED, FAILED)"
    )
    @ApiResponse(responseCode = "200", description = "Sync statistics")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getSyncStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("pending", eventRepository.countByStatus(EventStatus.PENDING));
        stats.put("completed", eventRepository.countByStatus(EventStatus.COMPLETED));
        stats.put("failed", eventRepository.countByStatus(EventStatus.FAILED));

        return ResponseEntity.ok(stats);
    }
}
