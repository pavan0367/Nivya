package com.nivya.admin.dto;

import com.nivya.role.RoleType;
import com.nivya.user.entity.User;
import com.nivya.user.entity.UserStatus;

import java.time.Instant;

/**
 * Summary representation of a user account for administrator lists.
 * Strictly excludes passwordHash, tokens, or credentials.
 */
public class AdminUserSummaryDto {

    private Long id;
    private String uuid;
    private String name;
    private String email;
    private RoleType role;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;
    private long deviceCount;
    private boolean hasActiveDevice;

    public AdminUserSummaryDto() {
    }

    public AdminUserSummaryDto(Long id, String uuid, String name, String email, RoleType role,
                               UserStatus status, Instant createdAt, Instant updatedAt,
                               Instant lastLoginAt, long deviceCount, boolean hasActiveDevice) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastLoginAt = lastLoginAt;
        this.deviceCount = deviceCount;
        this.hasActiveDevice = hasActiveDevice;
    }

    public static AdminUserSummaryDto fromEntity(User user, long deviceCount, boolean hasActiveDevice) {
        return new AdminUserSummaryDto(
                user.getId(),
                user.getUuid(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLoginAt(),
                deviceCount,
                hasActiveDevice
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public long getDeviceCount() {
        return deviceCount;
    }

    public void setDeviceCount(long deviceCount) {
        this.deviceCount = deviceCount;
    }

    public boolean isHasActiveDevice() {
        return hasActiveDevice;
    }

    public void setHasActiveDevice(boolean hasActiveDevice) {
        this.hasActiveDevice = hasActiveDevice;
    }
}
