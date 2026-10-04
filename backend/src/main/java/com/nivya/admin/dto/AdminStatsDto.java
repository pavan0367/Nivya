package com.nivya.admin.dto;

import java.util.List;

/**
 * High-level administrative system metrics.
 * Safe aggregate information exposing zero credentials, tokens, or private secrets.
 */
public class AdminStatsDto {

    private long totalUsers;
    private long totalParents;
    private long totalChildren;
    private long totalAdmins;
    private long activeUsers;
    private long disabledUsers;
    private long totalDevices;
    private long activeDevices;
    private long onlineDevices;
    private List<AdminUserSummaryDto> recentRegistrations;

    public AdminStatsDto() {
    }

    public AdminStatsDto(long totalUsers, long totalParents, long totalChildren, long totalAdmins,
                         long activeUsers, long disabledUsers, long totalDevices, long activeDevices,
                         long onlineDevices, List<AdminUserSummaryDto> recentRegistrations) {
        this.totalUsers = totalUsers;
        this.totalParents = totalParents;
        this.totalChildren = totalChildren;
        this.totalAdmins = totalAdmins;
        this.activeUsers = activeUsers;
        this.disabledUsers = disabledUsers;
        this.totalDevices = totalDevices;
        this.activeDevices = activeDevices;
        this.onlineDevices = onlineDevices;
        this.recentRegistrations = recentRegistrations;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalParents() {
        return totalParents;
    }

    public void setTotalParents(long totalParents) {
        this.totalParents = totalParents;
    }

    public long getTotalChildren() {
        return totalChildren;
    }

    public void setTotalChildren(long totalChildren) {
        this.totalChildren = totalChildren;
    }

    public long getTotalAdmins() {
        return totalAdmins;
    }

    public void setTotalAdmins(long totalAdmins) {
        this.totalAdmins = totalAdmins;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public long getDisabledUsers() {
        return disabledUsers;
    }

    public void setDisabledUsers(long disabledUsers) {
        this.disabledUsers = disabledUsers;
    }

    public long getTotalDevices() {
        return totalDevices;
    }

    public void setTotalDevices(long totalDevices) {
        this.totalDevices = totalDevices;
    }

    public long getActiveDevices() {
        return activeDevices;
    }

    public void setActiveDevices(long activeDevices) {
        this.activeDevices = activeDevices;
    }

    public long getOnlineDevices() {
        return onlineDevices;
    }

    public void setOnlineDevices(long onlineDevices) {
        this.onlineDevices = onlineDevices;
    }

    public List<AdminUserSummaryDto> getRecentRegistrations() {
        return recentRegistrations;
    }

    public void setRecentRegistrations(List<AdminUserSummaryDto> recentRegistrations) {
        this.recentRegistrations = recentRegistrations;
    }
}
