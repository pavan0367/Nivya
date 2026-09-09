package com.nivya.alerts.dto;

public class UnreadCountResponse {

    private long unreadCount;
    private Long familyId;

    public UnreadCountResponse() {
    }

    public UnreadCountResponse(long unreadCount, Long familyId) {
        this.unreadCount = unreadCount;
        this.familyId = familyId;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }
}
