package com.nivya.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class VerificationConfirmRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "A valid email address must be provided")
    private String email;

    @NotBlank(message = "Verification code is required")
    private String code;

    private String purpose = "EMAIL_VERIFICATION";

    public VerificationConfirmRequest() {
    }

    public VerificationConfirmRequest(String email, String code, String purpose) {
        this.email = email;
        this.code = code;
        this.purpose = purpose != null ? purpose : "EMAIL_VERIFICATION";
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}
