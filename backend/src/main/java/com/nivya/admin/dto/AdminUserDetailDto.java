package com.nivya.admin.dto;

import com.nivya.role.RoleType;
import com.nivya.user.entity.User;
import com.nivya.user.entity.UserStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Detailed administrative profile of a user account.
 * Excludes passwordHash and tokens.
 */
public class AdminUserDetailDto {

    private Long id;
    private String uuid;
    private String name;
    private String email;
    private String phone;
    private RoleType role;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;
    private long deviceCount;
    private List<AdminDeviceDto> devices = new ArrayList<>();
    private AdminFamilyDto family;
    private List<AdminAuditLogDto> recentAuditLogs = new ArrayList<>();

    public AdminUserDetailDto() {
    }

    public AdminUserDetailDto(Long id, String uuid, String name, String email, String phone,
                              RoleType role, UserStatus status, Instant createdAt, Instant updatedAt,
                              Instant lastLoginAt, long deviceCount, List<AdminDeviceDto> devices,
                              AdminFamilyDto family, List<AdminAuditLogDto> recentAuditLogs) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastLoginAt = lastLoginAt;
        this.deviceCount = deviceCount;
        this.devices = devices != null ? devices : new ArrayList<>();
        this.family = family;
        this.recentAuditLogs = recentAuditLogs != null ? recentAuditLogs : new ArrayList<>();
    }

    public static AdminUserDetailDto fromEntity(User user, List<AdminDeviceDto> devices,
                                                AdminFamilyDto family, List<AdminAuditLogDto> auditLogs) {
        return new AdminUserDetailDto(
                user.getId(),
                user.getUuid(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLoginAt(),
                devices != null ? devices.size() : 0,
                devices,
                family,
                auditLogs
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public List<AdminDeviceDto> getDevices() {
        return devices;
    }

    public void setDevices(List<AdminDeviceDto> devices) {
        this.devices = devices;
    }

    public AdminFamilyDto getFamily() {
        return family;
    }

    public void setFamily(AdminFamilyDto family) {
        this.family = family;
    }

    public List<AdminAuditLogDto> getRecentAuditLogs() {
        return recentAuditLogs;
    }

    public void setRecentAuditLogs(List<AdminAuditLogDto> recentAuditLogs) {
        this.recentAuditLogs = recentAuditLogs;
    }
}
