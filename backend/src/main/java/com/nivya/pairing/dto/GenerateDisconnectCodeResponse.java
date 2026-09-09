package com.nivya.pairing.dto;

import java.time.Instant;

public class GenerateDisconnectCodeResponse {

    private String code;
    private Instant expiresAt;
    private long ttlSeconds;

    public GenerateDisconnectCodeResponse() {
    }

    public GenerateDisconnectCodeResponse(String code, Instant expiresAt, long ttlSeconds) {
        this.code = code;
        this.expiresAt = expiresAt;
        this.ttlSeconds = ttlSeconds;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public long getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(long ttlSeconds) { this.ttlSeconds = ttlSeconds; }
}
