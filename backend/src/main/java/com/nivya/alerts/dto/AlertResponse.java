package com.nivya.alerts.dto;

import com.nivya.alerts.entity.Alert;

import java.time.Instant;

/**
 * Data transfer object representing a safety, connectivity, or hardware alert.
 */
public class AlertResponse {

    private Long id;
    private Long familyId;
    private Long deviceId;
    private String deviceName;
    private String deviceUuid;
    private String alertType;
    private String severity;
    private String title;
    private String message;
    private boolean resolved;
    private Instant resolvedAt;
    private boolean isRead;
    private Instant readAt;
    private String targetRole;
    private Instant createdAt;

    public AlertResponse() {
    }

    public static AlertResponse fromEntity(Alert alert) {
        AlertResponse res = new AlertResponse();
        res.setId(alert.getId());
        if (alert.getFamily() != null) {
            res.setFamilyId(alert.getFamily().getId());
        }
        if (alert.getDevice() != null) {
            res.setDeviceId(alert.getDevice().getId());
            res.setDeviceName(alert.getDevice().getDeviceName());
            res.setDeviceUuid(alert.getDevice().getDeviceUuid());
        }
        res.setAlertType(alert.getAlertType());
        res.setSeverity(alert.getSeverity());
        res.setTitle(alert.getTitle());
        res.setMessage(alert.getMessage());
        res.setResolved(alert.isResolved());
        res.setResolvedAt(alert.getResolvedAt());
        res.setRead(alert.isRead());
        res.setReadAt(alert.getReadAt());
        res.setTargetRole(alert.getTargetRole());
        res.setCreatedAt(alert.getCreatedAt());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
