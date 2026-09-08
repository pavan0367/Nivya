package com.nivya.role.controller;

import com.nivya.auth.dto.AuthResponse;
import com.nivya.common.response.ApiResponse;
import com.nivya.role.dto.RoleInfoResponse;
import com.nivya.role.dto.SelectRoleRequest;
import com.nivya.role.service.RoleService;
import com.nivya.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Role Management REST Controller for role selection, role verification, and navigation routing.
 */
@RestController
@RequestMapping("/api/v1/role")
@Tag(name = "Role Selection", description = "Endpoints for role selection, role metadata, and UI navigation routing")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "Select / Switch Role", description = "Updates user role, updates session, and re-issues refreshed JWT tokens")
    @PostMapping("/select")
    public ResponseEntity<ApiResponse<AuthResponse>> selectRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SelectRoleRequest request) {
        AuthResponse response = roleService.selectRole(principal, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Role selected successfully"));
    }

    @Operation(summary = "Get Current Role Info", description = "Retrieves authoritative role details and permitted/prohibited navigation modules")
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<RoleInfoResponse>> getCurrentRole(
            @AuthenticationPrincipal UserPrincipal principal) {
        RoleInfoResponse roleInfo = roleService.getCurrentRoleInfo(principal);
        return ResponseEntity.ok(ApiResponse.success(roleInfo, "Role configuration retrieved"));
    }
}
