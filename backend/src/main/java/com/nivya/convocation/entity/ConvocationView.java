package com.nivya.convocation.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "convocation_views")
public class ConvocationView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_uuid", nullable = false, unique = true, length = 64)
    private String sessionUuid;

    @Column(name = "view_started_at", nullable = false)
    private Instant viewStartedAt;

    @Column(name = "visibility_expires_at", nullable = false)
    private Instant visibilityExpiresAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ConvocationView() {
    }

    public ConvocationView(Long familyId, Long userId, String sessionUuid, Instant viewStartedAt, Instant visibilityExpiresAt) {
        this.familyId = familyId;
        this.userId = userId;
        this.sessionUuid = sessionUuid;
        this.viewStartedAt = viewStartedAt;
        this.visibilityExpiresAt = visibilityExpiresAt;
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
