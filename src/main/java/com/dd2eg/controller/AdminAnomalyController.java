package com.dd2eg.controller;

import com.dd2eg.DTO.AnomalyDetectionDTO;
import com.dd2eg.service.AnomalyDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for manual Anomaly Detection management.
 * Accessible only by administrators.
 */
@RestController
@RequestMapping("/api/admin/anomaly")
@Tag(name = "Admin Anomaly Detection", description = "API for managing Fraud/Anomaly detection")
public class AdminAnomalyController {

    private final AnomalyDetectionService anomalyDetectionService;

    public AdminAnomalyController(AnomalyDetectionService anomalyDetectionService) {
        this.anomalyDetectionService = anomalyDetectionService;
    }

    /**
     * Executes batch anomaly detection for ALL enterprises in the system.
     * GET /api/admin/anomaly/scan-all
     */
    @Operation(
            summary = "Execute scan for all enterprises",
            description = "Runs Query 5 on the entire graph to find fraud cycles."
    )
    @ApiResponse(responseCode = "200", description = "Scan completed")
    @GetMapping("/scan-all")
    public ResponseEntity<Map<String, Object>> scanAll() {
        List<AnomalyDetectionDTO> anomalies = anomalyDetectionService.detectAll();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Batch scan completed");
        response.put("anomaliesFound", anomalies.size());
        response.put("data", anomalies);

        return ResponseEntity.ok(response);
    }

    /**
     * Executes targeted anomaly detection for a specific enterprise.
     * GET /api/admin/anomaly/scan/{enterpriseId}
     */
    @Operation(
            summary = "Execute scan for a single enterprise",
            description = "Runs Query 5 specifically targeted at one company."
    )
    @ApiResponse(responseCode = "200", description = "Scan completed")
    @GetMapping("/scan/{enterpriseId}")
    public ResponseEntity<Map<String, Object>> scanForEnterprise(@PathVariable String enterpriseId) {
        List<AnomalyDetectionDTO> anomalies = anomalyDetectionService.detectForEnterprise(enterpriseId);

        Map<String, Object> response = new HashMap<>();
        response.put("enterpriseId", enterpriseId);
        response.put("message", "Targeted scan completed");
        response.put("anomaliesFound", anomalies.size());
        response.put("data", anomalies);

        return ResponseEntity.ok(response);
    }
}
