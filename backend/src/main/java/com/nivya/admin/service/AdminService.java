package com.nivya.admin.service;

import com.nivya.admin.dto.*;
import com.nivya.audit.entity.AuditLog;
import com.nivya.audit.repository.AuditLogRepository;
import com.nivya.audit.service.AuditService;
import com.nivya.auth.exception.DuplicateEmailException;
import com.nivya.auth.repository.RefreshTokenRepository;
import com.nivya.common.exception.ResourceNotFoundException;
import com.nivya.device.entity.Device;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.family.entity.Family;
import com.nivya.family.entity.FamilyMember;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.family.repository.FamilyRepository;
import com.nivya.role.RoleType;
import com.nivya.security.UserPrincipal;
import com.nivya.user.entity.User;
import com.nivya.user.entity.UserStatus;
import com.nivya.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Administrative operations service.
 * Enforces server-side privilege rules, anti-escalation safeguards, and immutable audit logs.
 */
@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyRepository familyRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final com.nivya.user.service.AccountDeletionService accountDeletionService;

    public AdminService(UserRepository userRepository,
                        DeviceRepository deviceRepository,
                        FamilyMemberRepository familyMemberRepository,
                        FamilyRepository familyRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        AuditLogRepository auditLogRepository,
                        AuditService auditService,
                        com.nivya.user.service.AccountDeletionService accountDeletionService) {
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.familyRepository = familyRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.accountDeletionService = accountDeletionService;
    }

    /**
     * Aggregates safe administrative metrics across users, roles, devices, and registrations.
     */
    @Transactional(readOnly = true)
    public AdminStatsDto getSystemStats() {
        long totalUsers = userRepository.count();
        long totalParents = userRepository.countByRole(RoleType.PARENT);
        long totalChildren = userRepository.countByRole(RoleType.CHILD);
        long totalAdmins = userRepository.countByRole(RoleType.ADMIN);
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long disabledUsers = userRepository.countByStatus(UserStatus.DISABLED);

        long totalDevices = deviceRepository.count();
        long activeDevices = deviceRepository.countByStatus("ACTIVE");
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(10));
        long onlineDevices = deviceRepository.countByLastSeenAtAfter(cutoff);

        List<User> topRecentUsers = userRepository.findTop10ByOrderByCreatedAtDesc();
        List<AdminUserSummaryDto> recentDtos = topRecentUsers.stream()
                .limit(5)
                .map(u -> {
                    long devCount = deviceRepository.countByUserId(u.getId());
                    return AdminUserSummaryDto.fromEntity(u, devCount, devCount > 0);
                })
                .toList();

        return new AdminStatsDto(
                totalUsers,
                totalParents,
                totalChildren,
                totalAdmins,
                activeUsers,
                disabledUsers,
                totalDevices,
                activeDevices,
                onlineDevices,
                recentDtos
        );
    }

    /**
     * Lists users with filtering, searching, and pagination.
     */
    @Transactional(readOnly = true)
    public Page<AdminUserSummaryDto> listUsers(String search, RoleType role, UserStatus status, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
                predicates.add(cb.or(nameMatch, emailMatch));
            }

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> users = userRepository.findAll(spec, pageable);

        return users.map(user -> {
            long deviceCount = deviceRepository.countByUserId(user.getId());
            return AdminUserSummaryDto.fromEntity(user, deviceCount, deviceCount > 0);
        });
    }

    /**
     * Retrieves full administrative details for a specific user.
     */
    @Transactional(readOnly = true)
    public AdminUserDetailDto getUserDetails(Long userId, UserPrincipal currentAdmin, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        List<Device> devices = deviceRepository.findByUserId(userId);
        List<AdminDeviceDto> deviceDtos = devices.stream().map(AdminDeviceDto::fromEntity).toList();

        AdminFamilyDto familyDto = null;
        Optional<FamilyMember> memberOpt = familyMemberRepository.findByUserId(userId);
        if (memberOpt.isPresent()) {
            FamilyMember member = memberOpt.get();
            Family family = member.getFamily();
            if (family != null) {
                List<FamilyMember> allMembers = familyMemberRepository.findByFamilyId(family.getId());
                familyDto = new AdminFamilyDto(
                        family.getId(),
                        family.getFamilyCode(),
                        family.getName(),
                        allMembers.size(),
                        member.getMemberRole() != null ? member.getMemberRole().name() : "MEMBER"
                );
            }
        }

        List<AuditLog> recentLogs = auditLogRepository.findTop10ByUserOrTargetUser(userId, PageRequest.of(0, 10));
        List<AdminAuditLogDto> auditDtos = recentLogs.stream().map(AdminAuditLogDto::fromEntity).toList();

        auditService.logAdminEvent(currentAdmin.getId(), userId, "ADMIN_USER_VIEW",
                "Viewed user profile details for ID: " + userId + " (" + user.getEmail() + ")", ipAddress);

        return AdminUserDetailDto.fromEntity(user, deviceDtos, familyDto, auditDtos);
    }

    /**
     * Modifies safe identity information of a user.
     */
    @Transactional
    public AdminUserDetailDto updateUser(Long userId, AdminUserUpdateRequest request,
                                         UserPrincipal currentAdmin, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        String normalizedEmail = request.getEmail().toLowerCase().trim();
        if (!user.getEmail().equalsIgnoreCase(normalizedEmail) && userRepository.existsByEmail(normalizedEmail)) {
            auditService.logAdminEvent(currentAdmin.getId(), userId, "ADMIN_ACTION_DENIED",
                    "Email conflict updating user ID " + userId + " to " + normalizedEmail, ipAddress);
            throw new DuplicateEmailException(normalizedEmail);
        }

        // Status update logic if status provided
        if (request.getStatus() != null && request.getStatus() != user.getStatus()) {
            validateStatusTransition(user, request.getStatus(), currentAdmin, ipAddress);
            if (request.getStatus() == UserStatus.DISABLED || request.getStatus() == UserStatus.SUSPENDED) {
                refreshTokenRepository.revokeAllUserTokens(user.getId(), Instant.now());
            }
            user.setStatus(request.getStatus());
        }

        String oldSummary = String.format("name='%s', email='%s', phone='%s', status=%s",
                user.getName(), user.getEmail(), user.getPhone(), user.getStatus());

        user.setName(request.getName().trim());
        user.setEmail(normalizedEmail);
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        user = userRepository.save(user);

        String newSummary = String.format("name='%s', email='%s', phone='%s', status=%s",
                user.getName(), user.getEmail(), user.getPhone(), user.getStatus());

        auditService.logAdminEvent(currentAdmin.getId(), userId, "ADMIN_USER_UPDATE",
                "Updated user ID " + userId + " from [" + oldSummary + "] to [" + newSummary + "]", ipAddress);

        return getUserDetails(userId, currentAdmin, ipAddress);
    }

    /**
     * Modifies the role of a user.
     * Prevents self-modification, invalid roles, and demotion of the final active admin.
     */
    @Transactional
    public AdminUserSummaryDto changeRole(Long userId, AdminRoleChangeRequest request,
                                         UserPrincipal currentAdmin, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.getId().equals(currentAdmin.getId())) {
            auditService.logAdminEvent(currentAdmin.getId(), userId, "ADMIN_ACTION_DENIED",
                    "Administrator attempted to modify their own role", ipAddress);
            throw new IllegalArgumentException("Administrators cannot modify their own role");
        }

        RoleType newRole = request.getRole();
        if (newRole == null) {
            throw new IllegalArgumentException("Role must be specified");
        }

        if (user.getRole() == RoleType.ADMIN && newRole != RoleType.ADMIN) {
            long activeAdmins = userRepository.countByRoleAndStatus(RoleType.ADMIN, UserStatus.ACTIVE);
            if (activeAdmins <= 1) {
                auditService.logAdminEvent(currentAdmin.getId(), userId, "ADMIN_ACTION_DENIED",
                        "Attempted to remove the final active administrator role", ipAddress);
                throw new IllegalStateException("Cannot change role of the final active administrator");
            }
        }

        RoleType oldRole = user.getRole();
        user.setRole(newRole);
        user = userRepository.save(user);

        auditService.logAdminEvent(currentAdmin.getId(), userId, "ADMIN_ROLE_CHANGE",
                "Changed role for user " + user.getEmail() + " from " + oldRole + " to " + newRole, ipAddress);

        long deviceCount = deviceRepository.countByUserId(user.getId());
        return AdminUserSummaryDto.fromEntity(user, deviceCount, deviceCount > 0);
    }

    /**
     * Modifies account status (e.g., ACTIVE <-> DISABLED).
     * Prevents self-disable and disabling the final active administrator.
     */
    @Transactional
    public AdminUserSummaryDto changeStatus(Long userId, AdminStatusChangeRequest request,
                                           UserPrincipal currentAdmin, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        UserStatus newStatus = request.getStatus();
        if (newStatus == null) {
            throw new IllegalArgumentException("Status must be specified");
        }

        validateStatusTransition(user, newStatus, currentAdmin, ipAddress);

        UserStatus oldStatus = user.getStatus();
        user.setStatus(newStatus);

        // If account is disabled or suspended, immediately invalidate all refresh tokens
        if (newStatus == UserStatus.DISABLED || newStatus == UserStatus.SUSPENDED) {
            refreshTokenRepository.revokeAllUserTokens(user.getId(), Instant.now());
            log.info("Revoked all active refresh tokens for user ID {} due to status change to {}", user.getId(), newStatus);
        }

        user = userRepository.save(user);

        auditService.logAdminEvent(currentAdmin.getId(), userId, "ADMIN_STATUS_CHANGE",
                "Changed status for user " + user.getEmail() + " from " + oldStatus + " to " + newStatus, ipAddress);

        long deviceCount = deviceRepository.countByUserId(user.getId());
        return AdminUserSummaryDto.fromEntity(user, deviceCount, deviceCount > 0);
    }

    /**
     * Lists audit logs with pagination and optional filtering by action or user.
     */
    @Transactional(readOnly = true)
    public Page<AdminAuditLogDto> listAuditLogs(Long userId, String action, Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                Predicate byActor = cb.equal(root.get("userId"), userId);
                Predicate byTarget = cb.equal(root.get("targetUserId"), userId);
                predicates.add(cb.or(byActor, byTarget));
            }

            if (action != null && !action.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("action"), action.trim()));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AuditLog> logs = auditLogRepository.findAll(spec, pageable);
        return logs.map(AdminAuditLogDto::fromEntity);
    }

    private void validateStatusTransition(User targetUser, UserStatus newStatus,
                                          UserPrincipal currentAdmin, String ipAddress) {
        if (targetUser.getId().equals(currentAdmin.getId()) && newStatus != UserStatus.ACTIVE) {
            auditService.logAdminEvent(currentAdmin.getId(), targetUser.getId(), "ADMIN_ACTION_DENIED",
                    "Administrator attempted to disable their own account", ipAddress);
            throw new IllegalArgumentException("Administrators cannot disable their own account");
        }

        if (targetUser.getRole() == RoleType.ADMIN && newStatus != UserStatus.ACTIVE) {
            long activeAdmins = userRepository.countByRoleAndStatus(RoleType.ADMIN, UserStatus.ACTIVE);
            if (activeAdmins <= 1) {
                auditService.logAdminEvent(currentAdmin.getId(), targetUser.getId(), "ADMIN_ACTION_DENIED",
                        "Attempted to disable the final active administrator", ipAddress);
                throw new IllegalStateException("Cannot disable the final active administrator");
            }
        }
    }

    /**
     * Permanently and irreversibly deletes a user account and cascades data removal (Admin only).
     * Prohibits self-deletion and deleting the final active administrator.
     */
    @Transactional
    public void deleteUser(Long userId, UserPrincipal currentAdmin, String ipAddress) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (targetUser.getId().equals(currentAdmin.getId())) {
            auditService.logAdminEvent(currentAdmin.getId(), targetUser.getId(), "ADMIN_ACTION_DENIED",
                    "Administrator attempted to delete their own account", ipAddress);
            throw new IllegalArgumentException("Administrators cannot delete their own account");
        }

        if (targetUser.getRole() == RoleType.ADMIN) {
            long activeAdmins = userRepository.countByRoleAndStatus(RoleType.ADMIN, UserStatus.ACTIVE);
            if (activeAdmins <= 1) {
                auditService.logAdminEvent(currentAdmin.getId(), targetUser.getId(), "ADMIN_ACTION_DENIED",
                        "Attempted to delete the final active administrator", ipAddress);
                throw new IllegalStateException("Cannot delete the final active administrator");
            }
        }

        String userEmail = targetUser.getEmail();
        RoleType role = targetUser.getRole();

        // Perform full cascade deletion via AccountDeletionService
        accountDeletionService.executeAdminAccountDeletion(targetUser, currentAdmin, ipAddress);

        auditService.logAdminEvent(currentAdmin.getId(), null, "ADMIN_USER_DELETE",
                "Permanently deleted user ID: " + userId + ", email: " + userEmail + ", role: " + role, ipAddress);
        log.info("Admin {} permanently deleted user ID {}, email: {}", currentAdmin.getEmail(), userId, userEmail);
    }
}
