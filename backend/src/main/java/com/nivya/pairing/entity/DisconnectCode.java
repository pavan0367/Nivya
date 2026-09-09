package com.nivya.pairing.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "disconnect_codes")
public class DisconnectCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "parent_user_id", nullable = false)
    private Long parentUserId;

    @Column(name = "child_user_id")
    private Long childUserId;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 5;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public DisconnectCode() {
    }

    public DisconnectCode(Long familyId, Long parentUserId, Long childUserId, String codeHash, Instant expiresAt) {
        this.familyId = familyId;
        this.parentUserId = parentUserId;
        this.childUserId = childUserId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.status = "PENDING";
        this.createdAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isExhausted() {
        return attempts >= maxAttempts;
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public void markUsed() {
        this.status = "USED";
        this.usedAt = Instant.now();
    }

    public void revoke() {
        this.status = "REVOKED";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFamilyId() { return familyId; }
    public void setFamilyId(Long familyId) { this.familyId = familyId; }

    public Long getParentUserId() { return parentUserId; }
    public void setParentUserId(Long parentUserId) { this.parentUserId = parentUserId; }

    public Long getChildUserId() { return childUserId; }
    public void setChildUserId(Long childUserId) { this.childUserId = childUserId; }

    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
