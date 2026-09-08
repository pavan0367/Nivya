package com.nivya.pairing.dto;

import com.nivya.role.RoleType;

import java.time.Instant;

public class LinkedMemberDto {

    private Long userId;
    private String name;
    private String email;
    private RoleType role;
    private Instant joinedAt;

    public LinkedMemberDto() {
    }

    public LinkedMemberDto(Long userId, String name, String email, RoleType role, Instant joinedAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}
