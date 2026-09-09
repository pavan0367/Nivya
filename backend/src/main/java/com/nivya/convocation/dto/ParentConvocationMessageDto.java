package com.nivya.convocation.dto;

import java.time.Instant;

public class ParentConvocationMessageDto {

    private Long id;
    private Long senderUserId;
    private String senderName;
    private Long receiverUserId;
    private String message;
    private boolean isChildOriginated;
    private Instant createdAt;
    private boolean seen;
    private Instant seenAt;

    public ParentConvocationMessageDto() {
    }

    public ParentConvocationMessageDto(Long id, Long senderUserId, String senderName, Long receiverUserId,
                                       String message, boolean isChildOriginated, Instant createdAt,
                                       boolean seen, Instant seenAt) {
        this.id = id;
        this.senderUserId = senderUserId;
        this.senderName = senderName;
        this.receiverUserId = receiverUserId;
        this.message = message;
        this.isChildOriginated = isChildOriginated;
        this.createdAt = createdAt;
        this.seen = seen;
        this.seenAt = seenAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(Long senderUserId) {
        this.senderUserId = senderUserId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Long getReceiverUserId() {
        return receiverUserId;
    }

    public void setReceiverUserId(Long receiverUserId) {
        this.receiverUserId = receiverUserId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isChildOriginated() {
        return isChildOriginated;
    }

    public void setChildOriginated(boolean childOriginated) {
        isChildOriginated = childOriginated;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public Instant getSeenAt() {
        return seenAt;
    }

    public void setSeenAt(Instant seenAt) {
        this.seenAt = seenAt;
    }
}
