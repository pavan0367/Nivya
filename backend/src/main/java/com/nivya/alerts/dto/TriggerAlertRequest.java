package com.nivya.alerts.dto;

import jakarta.validation.constraints.NotBlank;

public class TriggerAlertRequest {

    private String deviceUuid;

    @NotBlank(message = "Alert type is required")
    private String alertType;

    private String severity = "WARNING";

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private String targetRole = "PARENT";

    public TriggerAlertRequest() {
    }

    public TriggerAlertRequest(String deviceUuid, String alertType, String severity, String title, String message, String targetRole) {
        this.deviceUuid = deviceUuid;
        this.alertType = alertType;
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.targetRole = targetRole;
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

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }
}
