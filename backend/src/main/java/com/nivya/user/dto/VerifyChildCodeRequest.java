package com.nivya.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VerifyChildCodeRequest {

    @NotBlank(message = "Approval code is required")
    @Size(min = 6, max = 6, message = "Approval code must be exactly 6 digits")
    private String code;

    public VerifyChildCodeRequest() {
    }

    public VerifyChildCodeRequest(String code) {
        this.code = code;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
