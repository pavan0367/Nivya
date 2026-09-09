package com.nivya.alerts.service;

import com.nivya.alerts.dto.*;
import com.nivya.alerts.entity.Alert;
import com.nivya.alerts.entity.AlertRule;
import com.nivya.alerts.entity.NotificationRecord;
import com.nivya.alerts.repository.AlertRepository;
import com.nivya.alerts.repository.AlertRuleRepository;
import com.nivya.alerts.repository.NotificationRecordRepository;
import com.nivya.common.exception.ResourceNotFoundException;
import com.nivya.device.entity.Device;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.family.entity.Family;
import com.nivya.family.entity.FamilyMember;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.family.repository.FamilyRepository;
import com.nivya.notification.service.PushNotificationService;
import com.nivya.security.UserPrincipal;
import com.nivya.role.RoleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing alert rules, alert event generation, notification records,
 * and role-segregated visibility.
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final NotificationRecordRepository notificationRecordRepository;
    private final DeviceRepository deviceRepository;
    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PushNotificationService pushNotificationService;

    public AlertService(AlertRepository alertRepository,
                        AlertRuleRepository alertRuleRepository,
                        NotificationRecordRepository notificationRecordRepository,
                        DeviceRepository deviceRepository,
                        FamilyRepository familyRepository,
                        FamilyMemberRepository familyMemberRepository,
                        SimpMessagingTemplate messagingTemplate,
                        PushNotificationService pushNotificationService) {
        this.alertRepository = alertRepository;
        this.alertRuleRepository = alertRuleRepository;
        this.notificationRecordRepository = notificationRecordRepository;
        this.deviceRepository = deviceRepository;
        this.familyRepository = familyRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.messagingTemplate = messagingTemplate;
        this.pushNotificationService = pushNotificationService;
    }

    /**
     * Idempotently triggers a new alert or refreshes an active unresolved alert.
     */
    @Transactional
    public AlertResponse triggerOrUpdateAlert(Family family, Device device, String alertType,
                                             String severity, String title, String message, String targetRole) {
        Optional<Alert> existingActive = alertRepository.findFirstByDeviceIdAndAlertTypeAndResolvedFalse(device.getId(), alertType);

        Alert alert;
        if (existingActive.isPresent()) {
            alert = existingActive.get();
            alert.setSeverity(severity != null ? severity : alert.getSeverity());
            alert.setMessage(message);
            alert.setTitle(title);
            alert.setRead(false);
            if (targetRole != null) {
                alert.setTargetRole(targetRole);
            }
            alert = alertRepository.save(alert);
            log.info("Refreshed active alert id={} type={} for device={}", alert.getId(), alertType, device.getDeviceUuid());
        } else {
            alert = new Alert(family, device, alertType, severity != null ? severity : "WARNING", title, message, targetRole);
            alert = alertRepository.save(alert);
            log.info("Created new alert id={} type={} severity={} targetRole={} for device={}",
                    alert.getId(), alertType, severity, targetRole, device.getDeviceUuid());

            // Create notification records for targeted users
            dispatchNotificationRecords(alert, family, device, targetRole);
        }

        AlertResponse response = AlertResponse.fromEntity(alert);
        broadcastAlert(alert, response);
        return response;
    }

    /**
     * Resolves active alert for device and type if present.
     */
    @Transactional
    public void resolveAlert(Long deviceId, String alertType) {
        Optional<Alert> existing = alertRepository.findFirstByDeviceIdAndAlertTypeAndResolvedFalse(deviceId, alertType);
        existing.ifPresent(alert -> {
            alert.setResolved(true);
            alert.setResolvedAt(Instant.now());
            alertRepository.save(alert);
            log.info("Resolved alert id={} type={} for device={}", alert.getId(), alertType, deviceId);
            broadcastAlert(alert, AlertResponse.fromEntity(alert));
        });
    }

    /**
     * Parent endpoint: Retrieves family alerts with optional filtering by severity or unread status.
     */
    @Transactional(readOnly = true)
    public List<AlertResponse> getFamilyAlerts(Long familyId, Boolean unreadOnly, String severity, UserPrincipal principal) {
        validateParentAccess(familyId, principal);

        List<Alert> alerts;
        if (Boolean.TRUE.equals(unreadOnly)) {
            alerts = alertRepository.findByFamilyIdAndIsReadFalseOrderByCreatedAtDesc(familyId);
        } else if (severity != null && !severity.isBlank()) {
            alerts = alertRepository.findByFamilyIdAndSeverityOrderByCreatedAtDesc(familyId, severity.toUpperCase());
        } else {
            alerts = alertRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        }

        return alerts.stream().map(AlertResponse::fromEntity).collect(Collectors.toList());
    }

    /**
     * Child endpoint: Retrieves alerts strictly intended for this child device.
     */
    @Transactional(readOnly = true)
    public List<AlertResponse> getChildAlerts(UserPrincipal principal) {
        Optional<Device> childDeviceOpt = deviceRepository.findFirstByUserIdOrderByIdAsc(principal.getId());
        if (childDeviceOpt.isEmpty()) {
            return List.of();
        }
        Device childDevice = childDeviceOpt.get();

        // Children can ONLY view alerts where target_role is 'CHILD' or 'ALL'
        List<Alert> alerts = alertRepository.findByDeviceIdAndTargetRoleInOrderByCreatedAtDesc(
                childDevice.getId(), List.of("CHILD", "ALL"));

        return alerts.stream().map(AlertResponse::fromEntity).collect(Collectors.toList());
    }

    /**
     * Mark alert as read. Parents can mark family alerts; children can mark their own child-targeted alerts.
     */
    @Transactional
    public AlertResponse markAsRead(Long alertId, UserPrincipal principal) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with ID: " + alertId));

        validateUserAlertAccess(alert, principal, false);

        alert.setRead(true);
        alert.setReadAt(Instant.now());
        alert = alertRepository.save(alert);
        return AlertResponse.fromEntity(alert);
    }

    /**
     * Parent-only resolution: Resolves an alert incident.
     */
    @Transactional
    public AlertResponse resolveAlertById(Long alertId, UserPrincipal principal) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with ID: " + alertId));

        validateParentAccess(alert.getFamily().getId(), principal);

        alert.setResolved(true);
        alert.setResolvedAt(Instant.now());
        alert = alertRepository.save(alert);

        AlertResponse response = AlertResponse.fromEntity(alert);
        broadcastAlert(alert, response);
        return response;
    }

    /**
     * Retrieves or initializes default alert rules for a family.
     */
    @Transactional
    public List<AlertRuleDto> getAlertRules(Long familyId, UserPrincipal principal) {
        validateParentAccess(familyId, principal);

        List<AlertRule> rules = alertRuleRepository.findByFamilyId(familyId);
        if (rules.isEmpty()) {
            rules = initializeDefaultRules(familyId);
        }

        return rules.stream().map(AlertRuleDto::fromEntity).collect(Collectors.toList());
    }

    /**
     * Parent updates rule threshold, severity, or enabled toggle.
     */
    @Transactional
    public AlertRuleDto updateAlertRule(Long ruleId, UpdateAlertRuleRequest request, UserPrincipal principal) {
        AlertRule rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert rule not found with ID: " + ruleId));

        validateParentAccess(rule.getFamily().getId(), principal);

        if (request.getThresholdValue() != null && !request.getThresholdValue().isBlank()) {
            rule.setThresholdValue(request.getThresholdValue());
        }
        if (request.getSeverity() != null && !request.getSeverity().isBlank()) {
            rule.setSeverity(request.getSeverity().toUpperCase());
        }
        if (request.getEnabled() != null) {
            rule.setEnabled(request.getEnabled());
        }

        rule = alertRuleRepository.save(rule);
        return AlertRuleDto.fromEntity(rule);
    }

    /**
     * Unread count for current user badge.
     */
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UserPrincipal principal) {
        if (principal.getRole() == RoleType.PARENT) {
            Optional<FamilyMember> member = familyMemberRepository.findByUserId(principal.getId());
            if (member.isPresent()) {
                Long familyId = member.get().getFamily().getId();
                long count = alertRepository.countByFamilyIdAndIsReadFalse(familyId);
                return new UnreadCountResponse(count, familyId);
            }
            return new UnreadCountResponse(0, null);
        } else {
            Optional<Device> childDevice = deviceRepository.findFirstByUserIdOrderByIdAsc(principal.getId());
            if (childDevice.isPresent()) {
                long count = alertRepository.countByDeviceIdAndTargetRoleInAndIsReadFalse(
                        childDevice.get().getId(), List.of("CHILD", "ALL"));
                return new UnreadCountResponse(count, childDevice.get().getFamily() != null ? childDevice.get().getFamily().getId() : null);
            }
            return new UnreadCountResponse(0, null);
        }
    }

    private void dispatchNotificationRecords(Alert alert, Family family, Device device, String targetRole) {
        List<NotificationRecord> notifications = new ArrayList<>();

        if ("PARENT".equalsIgnoreCase(targetRole) || "ALL".equalsIgnoreCase(targetRole)) {
            List<FamilyMember> members = familyMemberRepository.findByFamilyId(family.getId());
            for (FamilyMember fm : members) {
                if (fm.getMemberRole() == RoleType.PARENT && fm.getUser() != null) {
                    notifications.add(new NotificationRecord(alert, fm.getUser(), "IN_APP", "SENT"));

                    // Dispatch push notifications to Parent's registered devices
                    List<Device> parentDevices = deviceRepository.findByUserId(fm.getUser().getId());
                    for (Device pDev : parentDevices) {
                        if (pDev.getPushToken() != null && !pDev.getPushToken().isBlank()) {
                            boolean sent = pushNotificationService.sendAlertNotification(
                                    pDev.getPushToken(),
                                    alert.getAlertType(),
                                    alert.getSeverity(),
                                    alert.getTitle(),
                                    alert.getMessage()
                            );
                            if (sent) {
                                notifications.add(new NotificationRecord(alert, fm.getUser(), "PUSH", "SENT"));
                            }
                        }
                    }
                }
            }
        }

        if (("CHILD".equalsIgnoreCase(targetRole) || "ALL".equalsIgnoreCase(targetRole)) && device.getUser() != null) {
            notifications.add(new NotificationRecord(alert, device.getUser(), "IN_APP", "SENT"));

            // Dispatch push notification to Child's registered device(s)
            List<Device> childDevices = deviceRepository.findByUserId(device.getUser().getId());
            for (Device cDev : childDevices) {
                if (cDev.getPushToken() != null && !cDev.getPushToken().isBlank()) {
                    boolean sent = pushNotificationService.sendAlertNotification(
                            cDev.getPushToken(),
                            alert.getAlertType(),
                            alert.getSeverity(),
                            alert.getTitle(),
                            alert.getMessage()
                    );
                    if (sent) {
                        notifications.add(new NotificationRecord(alert, device.getUser(), "PUSH", "SENT"));
                    }
                }
            }
        }

        if (!notifications.isEmpty()) {
            notificationRecordRepository.saveAll(notifications);
        }
    }

    private void broadcastAlert(Alert alert, AlertResponse response) {
        try {
            if (alert.getFamily() != null) {
                messagingTemplate.convertAndSend("/topic/alerts/family/" + alert.getFamily().getId(), response);
            }
            if (alert.getDevice() != null) {
                messagingTemplate.convertAndSend("/topic/alerts/device/" + alert.getDevice().getId(), response);
            }
        } catch (Exception e) {
            log.warn("Failed to broadcast alert event over WebSocket: {}", e.getMessage());
        }
    }

    private List<AlertRule> initializeDefaultRules(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family not found with ID: " + familyId));

        List<AlertRule> defaults = List.of(
                new AlertRule(family, "LOW_BATTERY", "15", "WARNING", "ALL", true),
                new AlertRule(family, "OFFLINE", "30", "WARNING", "PARENT", true),
                new AlertRule(family, "STALE_LOCATION", "15", "WARNING", "PARENT", true),
                new AlertRule(family, "PERMISSION_REVOKED", "ALL", "CRITICAL", "ALL", true),
                new AlertRule(family, "LOW_STORAGE", "10", "WARNING", "ALL", true),
                new AlertRule(family, "SECURITY_ALERT", "IMMEDIATE", "CRITICAL", "PARENT", true)
        );
        return alertRuleRepository.saveAll(defaults);
    }

    private void validateParentAccess(Long familyId, UserPrincipal principal) {
        if (principal.getRole() != RoleType.PARENT) {
            throw new AccessDeniedException("Only parent users can access family alert management controls");
        }
        Optional<FamilyMember> member = familyMemberRepository.findByFamilyIdAndUserId(familyId, principal.getId());
        if (member.isEmpty()) {
            throw new AccessDeniedException("User is not a member of this family");
        }
    }

    private void validateUserAlertAccess(Alert alert, UserPrincipal principal, boolean requireParent) {
        if (requireParent || principal.getRole() == RoleType.PARENT) {
            validateParentAccess(alert.getFamily().getId(), principal);
            return;
        }

        // Child validation: can only access alerts intended for their device and targetRole CHILD or ALL
        if (principal.getRole() == RoleType.CHILD) {
            if (alert.getDevice() == null || alert.getDevice().getUser() == null ||
                    !alert.getDevice().getUser().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Child not authorized to access alerts for this device");
            }
            if ("PARENT".equalsIgnoreCase(alert.getTargetRole())) {
                throw new AccessDeniedException("This alert is restricted to parent accounts");
            }
        }
    }
}
