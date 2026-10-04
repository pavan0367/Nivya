package com.nivya.admin.controller;

import com.nivya.admin.dto.*;
import com.nivya.admin.service.AdminService;
import com.nivya.common.response.ApiResponse;
import com.nivya.role.RoleType;
import com.nivya.security.UserPrincipal;
import com.nivya.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller providing secure Administrative management APIs.
 * Strictly restricted to users with ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Management", description = "Endpoints for authorized administrators to monitor and manage user accounts")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get Administrative Metrics", description = "Returns safe aggregate counts of users, roles, devices, and registrations")
    public ResponseEntity<ApiResponse<AdminStatsDto>> getSystemStats() {
        AdminStatsDto stats = adminService.getSystemStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "System statistics retrieved successfully"));
    }

    @GetMapping("/users")
    @Operation(summary = "List Users", description = "Returns paginated user list with optional search query, role filter, and status filter")
    public ResponseEntity<ApiResponse<Page<AdminUserSummaryDto>>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RoleType role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = ("lastLoginAt".equalsIgnoreCase(sortBy) || "name".equalsIgnoreCase(sortBy) || "email".equalsIgnoreCase(sortBy))
                ? sortBy : "createdAt";

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(direction, sortProperty));
        Page<AdminUserSummaryDto> users = adminService.listUsers(search, role, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(users, "Users retrieved successfully"));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get User Profile Details", description = "Retrieves full administrative profile for a specific user")
    public ResponseEntity<ApiResponse<AdminUserDetailDto>> getUserDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        AdminUserDetailDto user = adminService.getUserDetails(id, principal, getClientIp(request));
        return ResponseEntity.ok(ApiResponse.success(user, "User details retrieved successfully"));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Modify User Account", description = "Updates explicitly permitted identity fields of a user")
    public ResponseEntity<ApiResponse<AdminUserDetailDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserUpdateRequest updateRequest,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        AdminUserDetailDto updated = adminService.updateUser(id, updateRequest, principal, getClientIp(request));
        return ResponseEntity.ok(ApiResponse.success(updated, "User updated successfully"));
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Modify User Status", description = "Activates or disables an account. Prohibits self-disable and disabling final active admin")
    public ResponseEntity<ApiResponse<AdminUserSummaryDto>> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminStatusChangeRequest statusRequest,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        AdminUserSummaryDto summary = adminService.changeStatus(id, statusRequest, principal, getClientIp(request));
        return ResponseEntity.ok(ApiResponse.success(summary, "User status updated successfully"));
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Modify User Role", description = "Transitions user role. Prohibits self-modification and demoting the final active admin")
    public ResponseEntity<ApiResponse<AdminUserSummaryDto>> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody AdminRoleChangeRequest roleRequest,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        AdminUserSummaryDto summary = adminService.changeRole(id, roleRequest, principal, getClientIp(request));
        return ResponseEntity.ok(ApiResponse.success(summary, "User role updated successfully"));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "List Audit Logs", description = "Retrieves paginated administrative audit logs")
    public ResponseEntity<ApiResponse<Page<AdminAuditLogDto>>> listAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AdminAuditLogDto> logs = adminService.listAuditLogs(userId, action, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs, "Audit logs retrieved successfully"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
