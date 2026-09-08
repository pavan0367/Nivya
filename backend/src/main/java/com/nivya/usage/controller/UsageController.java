package com.nivya.usage.controller;

import com.nivya.common.response.ApiResponse;
import com.nivya.security.UserPrincipal;
import com.nivya.usage.dto.AppUsageResponse;
import com.nivya.usage.dto.UsageSummaryResponse;
import com.nivya.usage.dto.UsageTelemetryRequest;
import com.nivya.usage.dto.UsageTrendResponse;
import com.nivya.usage.service.UsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/usage")
@Tag(name = "Screen Time & App Usage", description = "Endpoints for device screen time, application usage breakdowns, and weekly trends")
public class UsageController {

    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    @PostMapping("/telemetry")
    @Operation(summary = "Submit screen time and app usage statistics", description = "Ingests aggregated foreground usage per app without capturing private messages or passwords")
    public ResponseEntity<ApiResponse<UsageSummaryResponse>> submitUsage(
            @Valid @RequestBody UsageTelemetryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UsageSummaryResponse response = usageService.recordUsage(request, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Usage telemetry recorded successfully"));
    }

    @GetMapping("/summary/{deviceId}")
    @Operation(summary = "Get daily screen time summary", description = "Retrieves total screen time and category breakdown for a device")
    public ResponseEntity<ApiResponse<UsageSummaryResponse>> getDailySummary(
            @PathVariable Long deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        UsageSummaryResponse response = usageService.getDailySummary(deviceId, date, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Daily usage summary retrieved"));
    }

    @GetMapping("/apps/{deviceId}")
    @Operation(summary = "Get detailed application usage breakdown", description = "Retrieves ranked list of applications with foreground durations")
    public ResponseEntity<ApiResponse<AppUsageResponse>> getAppUsage(
            @PathVariable Long deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUsageResponse response = usageService.getAppUsage(deviceId, date, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Application usage breakdown retrieved"));
    }

    @GetMapping("/trends/{deviceId}")
    @Operation(summary = "Get weekly usage trends", description = "Retrieves daily points for the past 7 days and percentage comparison with previous week")
    public ResponseEntity<ApiResponse<UsageTrendResponse>> getUsageTrends(
            @PathVariable Long deviceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        UsageTrendResponse response = usageService.getUsageTrends(deviceId, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Usage trends retrieved"));
    }
}
