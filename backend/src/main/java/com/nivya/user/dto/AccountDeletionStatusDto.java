package com.nivya.user.dto;

import com.nivya.role.RoleType;

public class AccountDeletionStatusDto {

    private RoleType role;
    private boolean isChild;
    private boolean hasConnectedParent;
    private String parentEmailMasked;
    private String message;
    private boolean hasPendingApprovalCode = false;
    private Long approvalCodeExpiresInSeconds;
    private String deliveryStatus = "IDLE";

    public AccountDeletionStatusDto() {
    }

    public AccountDeletionStatusDto(RoleType role, boolean isChild, boolean hasConnectedParent, String parentEmailMasked, String message) {
        this(role, isChild, hasConnectedParent, parentEmailMasked, message, false, null, "IDLE");
    }

    public AccountDeletionStatusDto(RoleType role, boolean isChild, boolean hasConnectedParent, String parentEmailMasked, String message, boolean hasPendingApprovalCode, Long approvalCodeExpiresInSeconds) {
        this(role, isChild, hasConnectedParent, parentEmailMasked, message, hasPendingApprovalCode, approvalCodeExpiresInSeconds, hasPendingApprovalCode ? "DELIVERED" : "IDLE");
    }

    public AccountDeletionStatusDto(RoleType role, boolean isChild, boolean hasConnectedParent, String parentEmailMasked, String message, boolean hasPendingApprovalCode, Long approvalCodeExpiresInSeconds, String deliveryStatus) {
        this.role = role;
        this.isChild = isChild;
        this.hasConnectedParent = hasConnectedParent;
        this.parentEmailMasked = parentEmailMasked;
        this.message = message;
        this.hasPendingApprovalCode = hasPendingApprovalCode;
        this.approvalCodeExpiresInSeconds = approvalCodeExpiresInSeconds;
        this.deliveryStatus = deliveryStatus != null ? deliveryStatus : "IDLE";
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

    public boolean isHasPendingApprovalCode() { return hasPendingApprovalCode; }
    public void setHasPendingApprovalCode(boolean hasPendingApprovalCode) { this.hasPendingApprovalCode = hasPendingApprovalCode; }

    public Long getApprovalCodeExpiresInSeconds() { return approvalCodeExpiresInSeconds; }
    public void setApprovalCodeExpiresInSeconds(Long approvalCodeExpiresInSeconds) { this.approvalCodeExpiresInSeconds = approvalCodeExpiresInSeconds; }

    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
}
