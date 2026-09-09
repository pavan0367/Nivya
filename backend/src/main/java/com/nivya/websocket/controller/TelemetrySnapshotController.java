package com.nivya.websocket.controller;

import com.nivya.common.response.ApiResponse;
import com.nivya.security.UserPrincipal;
import com.nivya.websocket.dto.DeviceTelemetrySnapshotDto;
import com.nivya.websocket.service.TelemetrySnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST and WebSocket STOMP controller for requesting consolidated device state snapshots after reconnection.
 */
@RestController
@RequestMapping("/api/v1/telemetry")
@Tag(name = "Telemetry Snapshot", description = "Endpoints for fast state snapshot rehydration after reconnection")
@SecurityRequirement(name = "Bearer Authentication")
public class TelemetrySnapshotController {

    private final TelemetrySnapshotService snapshotService;

    public TelemetrySnapshotController(TelemetrySnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @GetMapping("/snapshot/{deviceId}")
    @Operation(summary = "Get consolidated device snapshot", description = "Retrieves current battery, network, location, and activity telemetry in a single cached call")
    public ResponseEntity<ApiResponse<DeviceTelemetrySnapshotDto>> getDeviceSnapshot(
            @PathVariable Long deviceId,
            @AuthenticationPrincipal UserPrincipal principal) {

        DeviceTelemetrySnapshotDto snapshot = snapshotService.getDeviceSnapshot(deviceId, principal);
        return ResponseEntity.ok(ApiResponse.success(snapshot, "State snapshot retrieved successfully"));
    }

    @MessageMapping("/snapshot/{deviceId}")
    @SendToUser("/queue/snapshot")
    public DeviceTelemetrySnapshotDto handleWebSocketSnapshotRequest(
            @DestinationVariable Long deviceId,
            UserPrincipal principal) {

        return snapshotService.getDeviceSnapshot(deviceId, principal);
    }
}
