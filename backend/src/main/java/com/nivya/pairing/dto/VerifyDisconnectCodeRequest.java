package com.nivya.pairing.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyDisconnectCodeRequest {

    @NotBlank(message = "Disconnect code is required")
    private String code;

    public VerifyDisconnectCodeRequest() {
    }

    public VerifyDisconnectCodeRequest(String code) {
        this.code = code;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
