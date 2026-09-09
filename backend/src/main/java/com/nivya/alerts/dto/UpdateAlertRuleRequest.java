package com.nivya.alerts.dto;

public class UpdateAlertRuleRequest {

    private String thresholdValue;
    private String severity;
    private Boolean enabled;

    public UpdateAlertRuleRequest() {
    }

    public UpdateAlertRuleRequest(String thresholdValue, String severity, Boolean enabled) {
        this.thresholdValue = thresholdValue;
        this.severity = severity;
        this.enabled = enabled;
    }

    public String getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(String thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
