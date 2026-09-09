package com.nivya.auth.controller;

import com.nivya.auth.dto.*;
import com.nivya.auth.service.AuthService;
import com.nivya.common.response.ApiResponse;
import com.nivya.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication REST Controller managing registration, session login, token refresh, and logout.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, authentication, token refresh, and sessions")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register New Account", description = "Registers a new Parent or Child account and issues initial tokens")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                              HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(request, getClientIp(httpRequest));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User registered successfully"));
    }

    @Operation(summary = "User Login", description = "Authenticates user credentials and returns JWT access + refresh tokens")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @Operation(summary = "Refresh Access Token", description = "Exchanges a valid refresh token for rotated tokens")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                            HttpServletRequest httpRequest) {
        AuthResponse response = authService.refreshToken(request, getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @Operation(summary = "User Logout", description = "Invalidates active refresh tokens and revokes user session")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        String tokenString = request != null ? request.getRefreshToken() : null;
        authService.logout(tokenString, principal, getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @Operation(summary = "Current User Profile", description = "Retrieves profile and role of authenticated user")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSummaryDto>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserSummaryDto userDto = authService.getCurrentUser(principal);
        return ResponseEntity.ok(ApiResponse.success(userDto, "User context retrieved"));
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
        }
        return xfHeader.split(",")[0].trim();
    }
}
