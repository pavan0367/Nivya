package com.nivya.location.controller;

import com.nivya.common.response.ApiResponse;
import com.nivya.location.dto.LocationHistoryResponse;
import com.nivya.location.dto.LocationStatusResponse;
import com.nivya.location.dto.LocationTelemetryRequest;
import com.nivya.location.service.LocationService;
import com.nivya.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/location")
@Tag(name = "Location Telemetry", description = "Endpoints for real-time device location, consented location history, and availability states")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping("/telemetry")
    @Operation(summary = "Submit location telemetry", description = "Ingests GPS/network location with accuracy, provider, and permission state")
    public ResponseEntity<ApiResponse<LocationStatusResponse>> submitLocation(
            @Valid @RequestBody LocationTelemetryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        LocationStatusResponse response = locationService.recordLocation(request, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Location recorded successfully"));
    }

    @GetMapping("/current/{deviceId}")
    @Operation(summary = "Get current or last-known device location", description = "Retrieves real-time or last-known coordinates with stale state detection")
    public ResponseEntity<ApiResponse<LocationStatusResponse>> getCurrentLocation(
            @PathVariable Long deviceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        LocationStatusResponse response = locationService.getCurrentLocation(deviceId, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Current location retrieved successfully"));
    }

    @GetMapping("/history/{deviceId}")
    @Operation(summary = "Get location history breadcrumbs", description = "Retrieves time-indexed location points where family consent is granted")
    public ResponseEntity<ApiResponse<LocationHistoryResponse>> getLocationHistory(
            @PathVariable Long deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @AuthenticationPrincipal UserPrincipal principal) {
        LocationHistoryResponse response = locationService.getLocationHistory(deviceId, startTime, endTime, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Location history retrieved successfully"));
    }

    @PostMapping("/purge")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Purge historical locations", description = "Purges location records older than the specified retention window")
    public ResponseEntity<ApiResponse<Integer>> purgeOldLocations(
            @RequestParam(defaultValue = "30") int retentionDays) {
        int deleted = locationService.purgeOldLocations(retentionDays);
        return ResponseEntity.ok(ApiResponse.success(deleted, "Purged " + deleted + " records older than " + retentionDays + " days"));
    }
}
