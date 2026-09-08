package com.nivya.auth.dto;

import com.nivya.role.RoleType;
import com.nivya.user.entity.User;
import com.nivya.user.entity.UserStatus;

/**
 * Public User summary DTO for API responses.
 */
public class UserSummaryDto {

    private Long id;
    private String uuid;
    private String name;
    private String email;
    private RoleType role;
    private UserStatus status;

    public UserSummaryDto() {
    }

    public UserSummaryDto(Long id, String uuid, String name, String email, RoleType role, UserStatus status) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.status = status;
    }

    public static UserSummaryDto fromEntity(User user) {
        return new UserSummaryDto(
                user.getId(),
                user.getUuid(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}
