package com.nivya.network.controller;

import com.nivya.common.response.ApiResponse;
import com.nivya.network.dto.NetworkHistoryResponse;
import com.nivya.network.dto.NetworkStatusResponse;
import com.nivya.network.dto.NetworkTelemetryRequest;
import com.nivya.network.service.NetworkService;
import com.nivya.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/network")
@Tag(name = "Network Telemetry", description = "Endpoints for device network state collection, connectivity diagnostics, and history")
public class NetworkController {

    private final NetworkService networkService;

    public NetworkController(NetworkService networkService) {
        this.networkService = networkService;
    }

    @PostMapping("/telemetry")
    @Operation(summary = "Submit device network telemetry", description = "Ingests real-time network type, internet reachability, signal level, and quality")
    public ResponseEntity<ApiResponse<NetworkStatusResponse>> submitTelemetry(
            @Valid @RequestBody NetworkTelemetryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        NetworkStatusResponse response = networkService.recordTelemetry(request, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Network telemetry recorded successfully"));
    }

    @GetMapping("/current/{deviceId}")
    @Operation(summary = "Get current network status", description = "Retrieves the latest network status and connection parameters for a device")
    public ResponseEntity<ApiResponse<NetworkStatusResponse>> getCurrentNetwork(
            @PathVariable Long deviceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        NetworkStatusResponse response = networkService.getCurrentNetwork(deviceId, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Current network status retrieved"));
    }

    @GetMapping("/history/{deviceId}")
    @Operation(summary = "Get recent network history", description = "Retrieves chronological connectivity telemetry points for a device")
    public ResponseEntity<ApiResponse<NetworkHistoryResponse>> getNetworkHistory(
            @PathVariable Long deviceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        NetworkHistoryResponse response = networkService.getNetworkHistory(deviceId, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Network history retrieved"));
    }
}
