package com.nivya.alerts.controller;

import com.nivya.alerts.dto.*;
import com.nivya.alerts.service.AlertService;
import com.nivya.common.exception.ResourceNotFoundException;
import com.nivya.common.response.ApiResponse;
import com.nivya.device.entity.Device;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts & Notifications", description = "Endpoints for safety alerts, rules management, and notification audit")
@SecurityRequirement(name = "Bearer Authentication")
public class AlertController {

    private final AlertService alertService;
    private final DeviceRepository deviceRepository;

    public AlertController(AlertService alertService, DeviceRepository deviceRepository) {
        this.alertService = alertService;
        this.deviceRepository = deviceRepository;
    }

    @GetMapping("/family/{familyId}")
    @Operation(summary = "Get family alerts (Parent)", description = "Retrieves alerts for all devices linked to the family with optional filtering")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getFamilyAlerts(
            @PathVariable Long familyId,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) String severity,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<AlertResponse> alerts = alertService.getFamilyAlerts(familyId, unreadOnly, severity, principal);
        return ResponseEntity.ok(ApiResponse.success(alerts, "Family alerts retrieved successfully"));
    }

    @GetMapping("/my")
    @Operation(summary = "Get child alerts (Child)", description = "Retrieves personal alerts and friendly reminders intended for the child's device")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getChildAlerts(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<AlertResponse> alerts = alertService.getChildAlerts(principal);
        return ResponseEntity.ok(ApiResponse.success(alerts, "Personal alerts retrieved successfully"));
    }

    @PostMapping("/{alertId}/read")
    @Operation(summary = "Mark alert as read", description = "Marks a specific alert as read by the authorized user")
    public ResponseEntity<ApiResponse<AlertResponse>> markAsRead(
            @PathVariable Long alertId,
            @AuthenticationPrincipal UserPrincipal principal) {

        AlertResponse response = alertService.markAsRead(alertId, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Alert marked as read"));
    }

    @PostMapping("/{alertId}/resolve")
    @Operation(summary = "Resolve alert (Parent only)", description = "Resolves an active safety or telemetry alert incident")
    public ResponseEntity<ApiResponse<AlertResponse>> resolveAlert(
            @PathVariable Long alertId,
            @AuthenticationPrincipal UserPrincipal principal) {

        AlertResponse response = alertService.resolveAlertById(alertId, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Alert marked as resolved"));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread alerts count", description = "Returns the unread alerts badge count for the current user's role")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {

        UnreadCountResponse response = alertService.getUnreadCount(principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Unread count retrieved"));
    }

    @GetMapping("/rules/{familyId}")
    @Operation(summary = "Get alert rules (Parent)", description = "Retrieves configured threshold policies for the family")
    public ResponseEntity<ApiResponse<List<AlertRuleDto>>> getAlertRules(
            @PathVariable Long familyId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<AlertRuleDto> rules = alertService.getAlertRules(familyId, principal);
        return ResponseEntity.ok(ApiResponse.success(rules, "Alert rules retrieved successfully"));
    }

    @PutMapping("/rules/{ruleId}")
    @Operation(summary = "Update alert rule (Parent)", description = "Configures threshold, severity, or enabled state for a rule")
    public ResponseEntity<ApiResponse<AlertRuleDto>> updateAlertRule(
            @PathVariable Long ruleId,
            @RequestBody UpdateAlertRuleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        AlertRuleDto rule = alertService.updateAlertRule(ruleId, request, principal);
        return ResponseEntity.ok(ApiResponse.success(rule, "Alert rule updated successfully"));
    }

    @PostMapping("/trigger")
    @Operation(summary = "Trigger or log an alert", description = "Creates or refreshes an alert (e.g. security or status alert)")
    public ResponseEntity<ApiResponse<AlertResponse>> triggerAlert(
            @Valid @RequestBody TriggerAlertRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Device device = deviceRepository.findByDeviceUuid(request.getDeviceUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with UUID: " + request.getDeviceUuid()));

        if (device.getFamily() == null) {
            throw new ResourceNotFoundException("Device is not associated with a family");
        }

        AlertResponse response = alertService.triggerOrUpdateAlert(
                device.getFamily(),
                device,
                request.getAlertType(),
                request.getSeverity(),
                request.getTitle(),
                request.getMessage(),
                request.getTargetRole()
        );

        return ResponseEntity.ok(ApiResponse.success(response, "Alert triggered successfully"));
    }
}
