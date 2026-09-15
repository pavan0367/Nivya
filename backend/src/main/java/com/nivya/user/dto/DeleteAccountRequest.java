package com.nivya.user.dto;

public class DeleteAccountRequest {

    private String password;
    private String approvalCode;

    public DeleteAccountRequest() {
    }

    public DeleteAccountRequest(String password, String approvalCode) {
        this.password = password;
        this.approvalCode = approvalCode;
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getApprovalCode() { return approvalCode; }
    public void setApprovalCode(String approvalCode) { this.approvalCode = approvalCode; }
}
