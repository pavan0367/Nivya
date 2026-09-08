package com.nivya.role.dto;

import com.nivya.role.RoleType;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for selecting or updating a user's role.
 */
public class SelectRoleRequest {

    @NotNull(message = "Role must be specified (PARENT or CHILD)")
    private RoleType role;

    public SelectRoleRequest() {
    }

    public SelectRoleRequest(RoleType role) {
        this.role = role;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }
}
