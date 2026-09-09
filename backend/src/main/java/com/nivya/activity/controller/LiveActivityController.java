package com.nivya.activity.controller;

import com.nivya.activity.dto.ActivityEventDto;
import com.nivya.activity.dto.LiveActivityRequest;
import com.nivya.activity.dto.LiveActivityResponse;
import com.nivya.activity.service.LiveActivityService;
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
@RequestMapping("/api/v1/activity")
@Tag(name = "Live Activity", description = "Parent-only live activity and legitimate foreground telemetry ingestion")
@SecurityRequirement(name = "Bearer Authentication")
public class LiveActivityController {

    private final LiveActivityService liveActivityService;

    public LiveActivityController(LiveActivityService liveActivityService) {
        this.liveActivityService = liveActivityService;
    }

    @PostMapping("/telemetry")
    @Operation(summary = "Submit activity telemetry", description = "Records legitimate high-level activity telemetry from enrolled device")
    public ResponseEntity<ApiResponse<ActivityEventDto>> submitTelemetry(
            @Valid @RequestBody LiveActivityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ActivityEventDto event = liveActivityService.recordActivity(request, principal);
        return ResponseEntity.ok(ApiResponse.success(event, "Activity telemetry recorded successfully"));
    }

    @GetMapping("/live/{deviceId}")
    @Operation(summary = "Get live activity (Parent only)", description = "Retrieves real-time foreground activity, elapsed duration, and chronological activity timeline")
    public ResponseEntity<ApiResponse<LiveActivityResponse>> getLiveActivity(
            @PathVariable Long deviceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        LiveActivityResponse response = liveActivityService.getLiveActivity(deviceId, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Live activity retrieved successfully"));
    }
}
