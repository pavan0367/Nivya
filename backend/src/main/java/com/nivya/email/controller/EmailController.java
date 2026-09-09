package com.nivya.email.controller;

import com.nivya.common.response.ApiResponse;
import com.nivya.email.dto.*;
import com.nivya.email.service.EmailService;
import com.nivya.security.UserPrincipal;
import com.nivya.user.entity.User;
import com.nivya.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/email")
@Tag(name = "Email", description = "Endpoints for email verification, delivery logging, and notification preferences")
public class EmailController {

    private final EmailService emailService;
    private final UserRepository userRepository;

    public EmailController(EmailService emailService, UserRepository userRepository) {
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    @GetMapping("/preferences")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get Email Preferences", description = "Retrieves email notification preferences for authenticated user")
    public ResponseEntity<ApiResponse<EmailPreferenceDto>> getPreferences(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        EmailPreferenceDto prefs = emailService.getPreferences(user);
        return ResponseEntity.ok(ApiResponse.success(prefs, "Email preferences retrieved"));
    }

    @PutMapping("/preferences")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Update Email Preferences", description = "Updates optional email notification toggles")
    public ResponseEntity<ApiResponse<EmailPreferenceDto>> updatePreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EmailPreferenceDto request) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        EmailPreferenceDto updated = emailService.updatePreferences(user, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Email preferences updated"));
    }

    @PostMapping("/verify/send")
    @Operation(summary = "Send Verification Code", description = "Generates and emails a 15-minute, single-use, rate-limited code")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendVerificationCode(
            @Valid @RequestBody VerificationCodeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = principal != null ? userRepository.findById(principal.getId()).orElse(null) : null;
        emailService.generateVerificationCode(request.getEmail(), request.getPurpose(), user);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("message", "Verification code dispatched"),
                "Verification code dispatched to " + request.getEmail()
        ));
    }

    @PostMapping("/verify/confirm")
    @Operation(summary = "Confirm Verification Code", description = "Validates the one-time code against secure server hash")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> confirmVerificationCode(
            @Valid @RequestBody VerificationConfirmRequest request) {
        boolean valid = emailService.verifyCode(request.getEmail(), request.getCode(), request.getPurpose());
        if (!valid) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid, expired, or exhausted verification code"));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("verified", true), "Email verified successfully"));
    }

    @PostMapping("/notify-app-update")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Trigger App Update Email", description = "Broadcasts configurable app update email to opted-in users")
    public ResponseEntity<ApiResponse<Void>> notifyAppUpdate(
            @RequestParam String version,
            @RequestParam String releaseNotes) {
        emailService.sendAppUpdateNotificationAsync(version, releaseNotes);
        return ResponseEntity.ok(ApiResponse.success(null, "App update notifications queued for dispatch"));
    }
}
