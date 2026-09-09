package com.nivya.device.controller;

import com.nivya.common.response.ApiResponse;
import com.nivya.device.dto.DeviceHealthResponse;
import com.nivya.device.dto.DeviceHealthTelemetryRequest;
import com.nivya.device.service.DeviceHealthService;
import com.nivya.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/device/health")
@Tag(name = "Device Health", description = "Endpoints for system diagnostics, hardware health, storage, RAM, and permission health")
@SecurityRequirement(name = "Bearer Authentication")
public class DeviceHealthController {

    private final DeviceHealthService deviceHealthService;

    public DeviceHealthController(DeviceHealthService deviceHealthService) {
        this.deviceHealthService = deviceHealthService;
    }

    @PostMapping("/telemetry")
    @Operation(summary = "Record device health telemetry", description = "Ingests storage, RAM, battery, specs, and permission health diagnostics")
    public ResponseEntity<ApiResponse<DeviceHealthResponse>> recordTelemetry(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeviceHealthTelemetryRequest request) {

        DeviceHealthResponse response = deviceHealthService.recordTelemetry(request, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Device health telemetry recorded successfully"));
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "Get device health", description = "Retrieves full health diagnostics for parent or child view")
    public ResponseEntity<ApiResponse<DeviceHealthResponse>> getDeviceHealth(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long deviceId) {

        DeviceHealthResponse response = deviceHealthService.getDeviceHealth(deviceId, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Device health retrieved"));
    }

    @GetMapping("/my")
    @Operation(summary = "Get own device health", description = "Retrieves current authenticated device health diagnostics")
    public ResponseEntity<ApiResponse<DeviceHealthResponse>> getMyDeviceHealth(
            @AuthenticationPrincipal UserPrincipal principal) {

        DeviceHealthResponse response = deviceHealthService.getMyDeviceHealth(principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Own device health retrieved"));
    }
}
