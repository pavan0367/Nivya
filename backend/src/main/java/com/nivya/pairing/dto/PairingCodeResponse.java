package com.nivya.pairing.dto;

import com.nivya.role.RoleType;

import java.time.Instant;

public class PairingCodeResponse {

    private String code;
    private RoleType myRole;
    private RoleType targetRole;
    private Instant expiresAt;
    private long ttlSeconds;

    public PairingCodeResponse() {
    }

    public PairingCodeResponse(String code, RoleType myRole, RoleType targetRole, Instant expiresAt, long ttlSeconds) {
        this.code = code;
        this.myRole = myRole;
        this.targetRole = targetRole;
        this.expiresAt = expiresAt;
        this.ttlSeconds = ttlSeconds;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public RoleType getMyRole() {
        return myRole;
    }

    public void setMyRole(RoleType myRole) {
        this.myRole = myRole;
    }

    public RoleType getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(RoleType targetRole) {
        this.targetRole = targetRole;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
}
