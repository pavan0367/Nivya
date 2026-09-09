package com.nivya.security.authorization;

import com.nivya.device.entity.Device;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.role.RoleType;
import com.nivya.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Centralized authorization validator enforcing device ownership, sibling isolation,
 * and family boundary protection.
 *
 * Security Invariants:
 * 1. CHILD: Can ONLY access their own enrolled device (device.getUser().getId() == principal.getId()).
 *    Strictly forbidden from accessing siblings' devices, parent's devices, or foreign devices.
 * 2. PARENT: Can access any device belonging to their active family unit.
 *    Strictly forbidden from accessing devices belonging to foreign families or unlinked devices.
 */
@Component
public class DeviceAccessValidator {

    private static final Logger log = LoggerFactory.getLogger(DeviceAccessValidator.class);

    private final FamilyMemberRepository familyMemberRepository;

    public DeviceAccessValidator(FamilyMemberRepository familyMemberRepository) {
        this.familyMemberRepository = familyMemberRepository;
    }

    /**
     * Enforces strict device ownership and family isolation.
     * Throws AccessDeniedException if the user does not have permission.
     */
    public void validateDeviceAccess(Device device, UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required to access device telemetry");
        }
        if (device == null) {
            throw new AccessDeniedException("Invalid or non-existent device");
        }

        // 1. CHILD ACCOUNT: Strict device ownership check (Sibling isolation)
        if (principal.getRole() == RoleType.CHILD) {
            boolean isOwner = device.getUser() != null && device.getUser().getId().equals(principal.getId());
            if (!isOwner) {
                log.warn("Access Denied: Child user [{}] attempted unauthorized access to device [{}] owned by [{}]",
                        principal.getId(), device.getId(), device.getUser() != null ? device.getUser().getId() : "null");
                throw new AccessDeniedException("Access denied: Child accounts cannot access other device data");
            }
            return;
        }

        // 2. PARENT ACCOUNT: Strict family membership check (Cross-family isolation)
        if (principal.getRole() == RoleType.PARENT) {
            if (device.getFamily() == null) {
                log.warn("Access Denied: Parent user [{}] attempted access to unlinked device [{}]",
                        principal.getId(), device.getId());
                throw new AccessDeniedException("Access denied: Device is not associated with any family");
            }

            boolean isFamilyMember = familyMemberRepository.findByFamilyIdAndUserId(
                    device.getFamily().getId(), principal.getId()).isPresent();
            if (!isFamilyMember) {
                log.warn("Access Denied: Parent user [{}] attempted access to device [{}] belonging to foreign family [{}]",
                        principal.getId(), device.getId(), device.getFamily().getId());
                throw new AccessDeniedException("Access denied: Parent cannot access data for devices outside their family");
            }
            return;
        }

        throw new AccessDeniedException("Access denied: Unknown or unauthorized role " + principal.getRole());
    }

    /**
     * Validates that the principal is a PARENT belonging to the specified family.
     */
    public void validateFamilyAccess(Long familyId, UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (principal.getRole() != RoleType.PARENT) {
            throw new AccessDeniedException("Access denied: Only Parent accounts can manage family controls");
        }
        if (familyId == null || !familyMemberRepository.findByFamilyIdAndUserId(familyId, principal.getId()).isPresent()) {
            throw new AccessDeniedException("Access denied: User does not belong to family " + familyId);
        }
    }

    /**
     * Validates that the caller has PARENT role.
     */
    public void validateParentOnly(UserPrincipal principal) {
        if (principal == null || principal.getRole() != RoleType.PARENT) {
            throw new AccessDeniedException("Access denied: Parent-only operation");
        }
    }
}
