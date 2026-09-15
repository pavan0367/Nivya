package com.nivya.user.dto;

import com.nivya.role.RoleType;

public class AccountDeletionStatusDto {

    private RoleType role;
    private boolean isChild;
    private boolean hasConnectedParent;
    private String parentEmailMasked;
    private String message;

    public AccountDeletionStatusDto() {
    }

    public AccountDeletionStatusDto(RoleType role, boolean isChild, boolean hasConnectedParent, String parentEmailMasked, String message) {
        this.role = role;
        this.isChild = isChild;
        this.hasConnectedParent = hasConnectedParent;
        this.parentEmailMasked = parentEmailMasked;
        this.message = message;
    }

    public RoleType getRole() { return role; }
    public void setRole(RoleType role) { this.role = role; }

    public boolean isChild() { return isChild; }
    public void setChild(boolean child) { isChild = child; }

    public boolean isHasConnectedParent() { return hasConnectedParent; }
    public void setHasConnectedParent(boolean hasConnectedParent) { this.hasConnectedParent = hasConnectedParent; }

    public String getParentEmailMasked() { return parentEmailMasked; }
    public void setParentEmailMasked(String parentEmailMasked) { this.parentEmailMasked = parentEmailMasked; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
