package com.nivya.admin.dto;

import com.nivya.role.RoleType;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for modifying user role.
 */
public class AdminRoleChangeRequest {

    @NotNull(message = "Role must be specified")
    private RoleType role;

    public AdminRoleChangeRequest() {
    }

    public AdminRoleChangeRequest(RoleType role) {
        this.role = role;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }
}
