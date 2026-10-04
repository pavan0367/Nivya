package com.nivya.admin.dto;

import com.nivya.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for modifying user status.
 */
public class AdminStatusChangeRequest {

    @NotNull(message = "Status must be specified")
    private UserStatus status;

    public AdminStatusChangeRequest() {
    }

    public AdminStatusChangeRequest(UserStatus status) {
        this.status = status;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}
