package com.nivya.convocation.dto;

import java.time.Instant;
import java.util.List;

public class ChildViewingSessionResponse {

    private String sessionUuid;
    private Instant viewStartedAt;
    private Instant visibilityExpiresAt;
    private long remainingSeconds;
    private List<ChildConvocationMessageDto> messages;

    public ChildViewingSessionResponse() {
    }

    public ChildViewingSessionResponse(String sessionUuid, Instant viewStartedAt, Instant visibilityExpiresAt,
                                       long remainingSeconds, List<ChildConvocationMessageDto> messages) {
        this.sessionUuid = sessionUuid;
        this.viewStartedAt = viewStartedAt;
        this.visibilityExpiresAt = visibilityExpiresAt;
        this.remainingSeconds = remainingSeconds;
        this.messages = messages;
    }

    public String getSessionUuid() {
        return sessionUuid;
    }

    public void setSessionUuid(String sessionUuid) {
        this.sessionUuid = sessionUuid;
    }

    public Instant getViewStartedAt() {
        return viewStartedAt;
    }

    public void setViewStartedAt(Instant viewStartedAt) {
        this.viewStartedAt = viewStartedAt;
    }

    public Instant getVisibilityExpiresAt() {
        return visibilityExpiresAt;
    }

    public void setVisibilityExpiresAt(Instant visibilityExpiresAt) {
        this.visibilityExpiresAt = visibilityExpiresAt;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public List<ChildConvocationMessageDto> getMessages() {
        return messages;
    }

    public void setMessages(List<ChildConvocationMessageDto> messages) {
        this.messages = messages;
    }
}
