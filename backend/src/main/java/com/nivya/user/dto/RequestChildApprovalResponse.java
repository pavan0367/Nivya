package com.nivya.user.dto;

public class RequestChildApprovalResponse {

    private String message;
    private String parentEmailMasked;
    private int expiresInMinutes;

    public RequestChildApprovalResponse() {
    }

    public RequestChildApprovalResponse(String message, String parentEmailMasked, int expiresInMinutes) {
        this.message = message;
        this.parentEmailMasked = parentEmailMasked;
        this.expiresInMinutes = expiresInMinutes;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getParentEmailMasked() { return parentEmailMasked; }
    public void setParentEmailMasked(String parentEmailMasked) { this.parentEmailMasked = parentEmailMasked; }

    public int getExpiresInMinutes() { return expiresInMinutes; }
    public void setExpiresInMinutes(int expiresInMinutes) { this.expiresInMinutes = expiresInMinutes; }
}
