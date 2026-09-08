package com.nivya.pairing.entity;

import com.nivya.role.RoleType;
import com.nivya.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * PairingRequest represents an ephemeral one-time connection token.
 */
@Entity
@Table(name = "pairing_requests")
public class PairingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private User requester;

    @Column(name = "connection_code", nullable = false, unique = true, length = 32)
    private String connectionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_role", nullable = false, length = 20)
    private RoleType targetRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PairingStatus status = PairingStatus.PENDING;

    @Column(name = "device_fingerprint", length = 100)
    private String deviceFingerprint;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by_user_id")
    private User acceptedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PairingRequest() {
    }

    public PairingRequest(User requester, String connectionCode, RoleType targetRole, Instant expiresAt, String deviceFingerprint) {
        this.requester = requester;
        this.connectionCode = connectionCode;
        this.targetRole = targetRole;
        this.expiresAt = expiresAt;
        this.deviceFingerprint = deviceFingerprint;
        this.status = PairingStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    public boolean isAvailable() {
        return this.status == PairingStatus.PENDING && !isExpired();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public String getConnectionCode() {
        return connectionCode;
    }

    public void setConnectionCode(String connectionCode) {
        this.connectionCode = connectionCode;
    }

    public RoleType getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(RoleType targetRole) {
        this.targetRole = targetRole;
    }

    public PairingStatus getStatus() {
        return status;
    }

    public void setStatus(PairingStatus status) {
        this.status = status;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public User getAcceptedBy() {
        return acceptedBy;
    }

    public void setAcceptedBy(User acceptedBy) {
        this.acceptedBy = acceptedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
