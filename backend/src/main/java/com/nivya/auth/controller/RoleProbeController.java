package com.nivya.auth.controller;

import com.nivya.common.response.ApiResponse;
import com.nivya.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller providing role-guarded test probes for verifying server-authoritative RBAC.
 */
@RestController
@RequestMapping("/api/v1")
@Hidden
public class RoleProbeController {

    @GetMapping("/parent/probe")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<String>> parentOnlyProbe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(principal.getEmail(), "Authorized as PARENT"));
    }

    @GetMapping("/child/probe")
    @PreAuthorize("hasRole('CHILD')")
    public ResponseEntity<ApiResponse<String>> childOnlyProbe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(principal.getEmail(), "Authorized as CHILD"));
    }
}
