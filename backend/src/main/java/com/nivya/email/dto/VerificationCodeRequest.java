package com.nivya.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class VerificationCodeRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "A valid email address must be provided")
    private String email;

    private String purpose = "EMAIL_VERIFICATION";

    public VerificationCodeRequest() {
    }

    public VerificationCodeRequest(String email, String purpose) {
        this.email = email;
        this.purpose = purpose != null ? purpose : "EMAIL_VERIFICATION";
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}
