package com.nivya.role;

/**
 * System roles in Nivya.
 * Role determines UI rendering, API access permissions, and device capabilities.
 */
public enum RoleType {
    PARENT,
    CHILD;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
