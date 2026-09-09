package com.nivya.convocation.dto;

public class ChildVisibilityStateResponse {

    private boolean viewingActive;
    private long remainingSeconds;
    private int unreadCount;

    public ChildVisibilityStateResponse() {
    }

    public ChildVisibilityStateResponse(boolean viewingActive, long remainingSeconds, int unreadCount) {
        this.viewingActive = viewingActive;
        this.remainingSeconds = remainingSeconds;
        this.unreadCount = unreadCount;
    }

    public boolean isViewingActive() {
        return viewingActive;
    }

    public void setViewingActive(boolean viewingActive) {
        this.viewingActive = viewingActive;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
