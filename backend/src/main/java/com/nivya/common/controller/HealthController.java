package com.nivya.common.controller;

import com.nivya.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health & Operational Monitoring REST Controller.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health & System", description = "Endpoints for platform liveness, readiness, and runtime metadata")
public class HealthController {

    @Value("${spring.application.name:nivya-backend}")
    private String applicationName;

    @Value("${nivya.version:1.0.0}")
    private String version;

    @Operation(summary = "System Health & Runtime Info", description = "Returns system liveness, active profile, and uptime")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthStatus() {
        Map<String, Object> healthInfo = new LinkedHashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("application", applicationName);
        healthInfo.put("version", version);
        healthInfo.put("timestamp", Instant.now().toString());
        healthInfo.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());

        return ResponseEntity.ok(ApiResponse.success(healthInfo, "Service is operational"));
    }

    @Operation(summary = "Fast Ping Check", description = "Minimal endpoint for fast connectivity checks")
    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.success("pong", "Ping successful"));
    }
}
