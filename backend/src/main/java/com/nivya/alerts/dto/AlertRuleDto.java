package com.nivya.alerts.dto;

import com.nivya.alerts.entity.AlertRule;

import java.time.Instant;

/**
 * DTO representing an Alert policy rule configuration.
 */
public class AlertRuleDto {

    private Long id;
    private Long familyId;
    private String ruleType;
    private String thresholdValue;
    private String severity;
    private String targetRole;
    private boolean enabled;
    private Instant updatedAt;

    public AlertRuleDto() {
    }

    public static AlertRuleDto fromEntity(AlertRule rule) {
        AlertRuleDto dto = new AlertRuleDto();
        dto.setId(rule.getId());
        if (rule.getFamily() != null) {
            dto.setFamilyId(rule.getFamily().getId());
        }
        dto.setRuleType(rule.getRuleType());
        dto.setThresholdValue(rule.getThresholdValue());
        dto.setSeverity(rule.getSeverity());
        dto.setTargetRole(rule.getTargetRole());
        dto.setEnabled(rule.isEnabled());
        dto.setUpdatedAt(rule.getUpdatedAt());
        return dto;
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

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
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

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
