package com.nivya.session.controller;

import com.nivya.common.response.ApiResponse;
import com.nivya.security.UserPrincipal;
import com.nivya.session.dto.DeviceSessionDto;
import com.nivya.session.service.DeviceSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "Sessions", description = "Endpoints for authenticated device session tracking and remote revocation")
@SecurityRequirement(name = "Bearer Authentication")
public class DeviceSessionController {

    private final DeviceSessionService sessionService;

    public DeviceSessionController(DeviceSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    @Operation(summary = "List Active Sessions", description = "Retrieves all authenticated device sessions for current user")
    public ResponseEntity<ApiResponse<List<DeviceSessionDto>>> getSessions(@AuthenticationPrincipal UserPrincipal principal) {
        List<DeviceSessionDto> sessions = sessionService.getUserSessions(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(sessions, "Device sessions retrieved"));
    }

    @PostMapping("/{sessionId}/revoke")
    @Operation(summary = "Revoke Device Session", description = "Remotely terminates and invalidates an active session")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long sessionId) {
        sessionService.revokeSession(principal.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.success(null, "Session revoked successfully"));
    }
}
