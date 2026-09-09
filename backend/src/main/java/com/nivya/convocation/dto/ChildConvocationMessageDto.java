package com.nivya.convocation.dto;

import java.time.Instant;

public class ChildConvocationMessageDto {

    private Long id;
    private String message;
    private Instant createdAt;

    public ChildConvocationMessageDto() {
    }

    public ChildConvocationMessageDto(Long id, String message, Instant createdAt) {
        this.id = id;
        this.message = message;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
