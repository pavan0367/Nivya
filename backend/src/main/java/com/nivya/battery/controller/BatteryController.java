package com.nivya.battery.controller;

import com.nivya.battery.dto.BatteryHistoryResponse;
import com.nivya.battery.dto.BatteryStatusResponse;
import com.nivya.battery.dto.BatteryTelemetryRequest;
import com.nivya.battery.dto.BatteryTrendResponse;
import com.nivya.battery.service.BatteryService;
import com.nivya.common.response.ApiResponse;
import com.nivya.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/battery")
@Tag(name = "Battery Telemetry", description = "Endpoints for battery status reporting, trends, and history")
@SecurityRequirement(name = "Bearer Authentication")
public class BatteryController {

    private final BatteryService batteryService;

    public BatteryController(BatteryService batteryService) {
        this.batteryService = batteryService;
    }

    @PostMapping("/telemetry")
    @Operation(summary = "Record battery telemetry", description = "Ingests battery level, charging state, health, and triggers alerts/WebSocket events")
    public ResponseEntity<ApiResponse<BatteryStatusResponse>> recordTelemetry(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BatteryTelemetryRequest request) {

        BatteryStatusResponse response = batteryService.recordTelemetry(request, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Battery telemetry recorded successfully"));
    }

    @GetMapping("/current/{deviceId}")
    @Operation(summary = "Get current battery status", description = "Retrieves the latest battery snapshot for a family device")
    public ResponseEntity<ApiResponse<BatteryStatusResponse>> getCurrentBattery(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long deviceId) {

        BatteryStatusResponse response = batteryService.getCurrentBattery(deviceId, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Battery status retrieved"));
    }

    @GetMapping("/history/{deviceId}")
    @Operation(summary = "Get battery history", description = "Retrieves chronological battery telemetry data points for charting")
    public ResponseEntity<ApiResponse<BatteryHistoryResponse>> getBatteryHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long deviceId) {

        BatteryHistoryResponse response = batteryService.getBatteryHistory(deviceId, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Battery history retrieved"));
    }

    @GetMapping("/trends/{deviceId}")
    @Operation(summary = "Get battery trends", description = "Calculates drain rate (%/hour), estimated battery lifetime, and temperature")
    public ResponseEntity<ApiResponse<BatteryTrendResponse>> getBatteryTrends(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long deviceId) {

        BatteryTrendResponse response = batteryService.getBatteryTrends(deviceId, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Battery trends calculated"));
    }
}
