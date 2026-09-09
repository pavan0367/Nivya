package com.nivya.convocation.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "convocation_messages")
public class ConvocationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    @Column(name = "receiver_user_id", nullable = false)
    private Long receiverUserId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "seen_at")
    private Instant seenAt;

    @Column(name = "view_started_at")
    private Instant viewStartedAt;

    @Column(name = "visibility_expires_at")
    private Instant visibilityExpiresAt;

    @Column(name = "child_visibility_expires_at")
    private Instant childVisibilityExpiresAt;

    @Column(nullable = false, length = 30)
    private String status = "UNREAD";

    public ConvocationMessage() {
    }

    public ConvocationMessage(Long familyId, Long senderUserId, Long receiverUserId, String message) {
        this.familyId = familyId;
        this.senderUserId = senderUserId;
        this.receiverUserId = receiverUserId;
        this.message = message;
        this.createdAt = Instant.now();
        this.status = "UNREAD";
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = "UNREAD";
        }
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

    public Long getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(Long senderUserId) {
        this.senderUserId = senderUserId;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public Instant getSeenAt() {
        return seenAt;
    }

    public void setSeenAt(Instant seenAt) {
        this.seenAt = seenAt;
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

    public Instant getChildVisibilityExpiresAt() {
        return childVisibilityExpiresAt;
    }

    public void setChildVisibilityExpiresAt(Instant childVisibilityExpiresAt) {
        this.childVisibilityExpiresAt = childVisibilityExpiresAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
