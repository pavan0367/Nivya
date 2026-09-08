package com.nivya.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh Token Request DTO.
 */
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token must not be blank")
    private String refreshToken;

    public RefreshTokenRequest() {
    }

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
