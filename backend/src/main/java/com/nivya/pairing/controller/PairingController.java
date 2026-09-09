package com.nivya.pairing.controller;

import com.nivya.common.response.ApiResponse;
import com.nivya.pairing.dto.*;
import com.nivya.pairing.service.PairingService;
import com.nivya.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pairing")
@Tag(name = "Pairing", description = "Endpoints for Parent-Child device pairing and connection management")
@SecurityRequirement(name = "Bearer Authentication")
public class PairingController {

    private final PairingService pairingService;

    public PairingController(PairingService pairingService) {
        this.pairingService = pairingService;
    }

    @PostMapping({"/code", "/create"})
    @Operation(summary = "Generate pairing code", description = "Generates an ephemeral, one-time 10-minute pairing code for the opposite role")
    public ResponseEntity<ApiResponse<PairingCodeResponse>> generateCode(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) GenerateCodeRequest request,
            HttpServletRequest servletRequest) {

        String ip = extractClientIp(servletRequest);
        PairingCodeResponse response = pairingService.generatePairingCode(principal, request, ip);
        return ResponseEntity.ok(ApiResponse.success(response, "Pairing code generated successfully"));
    }

    @PostMapping("/connect")
    @Operation(summary = "Connect devices", description = "Validates the opposite device's pairing code and establishes persistent family and device link")
    public ResponseEntity<ApiResponse<PairingStatusResponse>> connect(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ConnectPairingRequest request,
            HttpServletRequest servletRequest) {

        String ip = extractClientIp(servletRequest);
        PairingStatusResponse response = pairingService.connectDevices(principal, request, ip);
        return ResponseEntity.ok(ApiResponse.success(response, "Devices paired successfully"));
    }

    @GetMapping("/status")
    @Operation(summary = "Get pairing status", description = "Retrieves family relationship and online/offline status of all linked devices")
    public ResponseEntity<ApiResponse<PairingStatusResponse>> getStatus(
            @AuthenticationPrincipal UserPrincipal principal) {

        PairingStatusResponse response = pairingService.getPairingStatus(principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Pairing status retrieved"));
    }

    @PostMapping("/revoke")
    @Operation(summary = "Revoke device link", description = "Allows a Parent to unlink a device from the family unit")
    public ResponseEntity<ApiResponse<String>> revoke(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RevokePairingRequest request,
            HttpServletRequest servletRequest) {

        String ip = extractClientIp(servletRequest);
        pairingService.revokePairing(principal, request, ip);
        return ResponseEntity.ok(ApiResponse.success(null, "Device link revoked successfully"));
    }

    @PostMapping("/disconnect/code")
    @Operation(summary = "Generate Disconnect Code (Parent only)", description = "Generates a 10-minute, single-use disconnect code")
    public ResponseEntity<ApiResponse<GenerateDisconnectCodeResponse>> generateDisconnectCode(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest) {
        String ip = extractClientIp(servletRequest);
        GenerateDisconnectCodeResponse response = pairingService.generateDisconnectCode(principal, ip);
        return ResponseEntity.ok(ApiResponse.success(response, "Disconnect code generated successfully"));
    }

    @PostMapping("/disconnect/verify")
    @Operation(summary = "Verify Disconnect Code (Child only)", description = "Validates the Parent-generated disconnect code and unlinks connection")
    public ResponseEntity<ApiResponse<Void>> verifyDisconnectCode(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VerifyDisconnectCodeRequest request,
            HttpServletRequest servletRequest) {
        String ip = extractClientIp(servletRequest);
        pairingService.verifyDisconnectCode(principal, request, ip);
        return ResponseEntity.ok(ApiResponse.success(null, "Relationship disconnected successfully"));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
