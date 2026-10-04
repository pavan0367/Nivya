package com.nivya.admin.dto;

import com.nivya.audit.entity.AuditLog;

import java.time.Instant;

/**
 * Administrative projection of an audit log entry.
 */
public class AdminAuditLogDto {

    private Long id;
    private Long userId;
    private Long targetUserId;
    private String action;
    private String details;
    private String ipAddress;
    private Instant timestamp;

    public AdminAuditLogDto() {
    }

    public AdminAuditLogDto(Long id, Long userId, Long targetUserId, String action,
                            String details, String ipAddress, Instant timestamp) {
        this.id = id;
        this.userId = userId;
        this.targetUserId = targetUserId;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
    }

    public static AdminAuditLogDto fromEntity(AuditLog log) {
        return new AdminAuditLogDto(
                log.getId(),
                log.getUserId(),
                log.getTargetUserId(),
                log.getAction(),
                log.getDetails(),
                log.getIpAddress(),
                log.getTimestamp()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
