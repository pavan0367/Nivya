package com.nivya.user.controller;

import com.nivya.common.response.ApiResponse;
import com.nivya.security.UserPrincipal;
import com.nivya.user.dto.AccountDeletionStatusDto;
import com.nivya.user.dto.DeleteAccountRequest;
import com.nivya.user.dto.RequestChildApprovalResponse;
import com.nivya.user.dto.VerifyChildCodeRequest;
import com.nivya.user.entity.User;
import com.nivya.user.repository.UserRepository;
import com.nivya.user.service.AccountDeletionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/account")
@Tag(name = "Account Deletion", description = "Endpoints for permanent account deletion and child-parent deletion approval")
@SecurityRequirement(name = "Bearer Authentication")
public class AccountDeletionController {

    private final AccountDeletionService deletionService;
    private final UserRepository userRepository;

    public AccountDeletionController(AccountDeletionService deletionService, UserRepository userRepository) {
        this.deletionService = deletionService;
        this.userRepository = userRepository;
    }

    @GetMapping("/deletion/status")
    @Operation(summary = "Get Deletion Status", description = "Checks whether user is a parent or child, and whether parent approval is required")
    public ResponseEntity<ApiResponse<AccountDeletionStatusDto>> getDeletionStatus(
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        AccountDeletionStatusDto status = deletionService.getDeletionStatus(user);
        return ResponseEntity.ok(ApiResponse.success(status, "Deletion status retrieved"));
    }

    @PostMapping("/deletion/request-child-approval")
    @Operation(summary = "Request Child Deletion Approval", description = "Generates a 6-digit approval code sent to connected parent's email")
    public ResponseEntity<ApiResponse<RequestChildApprovalResponse>> requestChildApproval(
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        RequestChildApprovalResponse response = deletionService.requestChildDeletionApproval(user);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/deletion/verify-child-code")
    @Operation(summary = "Verify Child Deletion Code", description = "Validates the 6-digit approval code before final confirmation")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> verifyChildCode(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VerifyChildCodeRequest request) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean valid = deletionService.verifyChildApprovalCode(user, request.getCode());
        if (!valid) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid, expired, or exhausted parent approval code"));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("approved", true), "Approval code verified successfully"));
    }

    @PostMapping("/delete")
    @Operation(summary = "Permanently Delete Account", description = "Permanently deletes user account, email identity, and credentials")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) DeleteAccountRequest request) {
        if (principal == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        deletionService.executeAccountDeletion(user, request != null ? request : new DeleteAccountRequest());
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("message", "Account permanently deleted"),
                "Your Nivya account has been permanently deleted."
        ));
    }
}
