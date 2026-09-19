package com.nivya.user.service;

import com.nivya.audit.service.AuditService;
import com.nivya.auth.repository.RefreshTokenRepository;
import com.nivya.device.entity.Device;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.email.dto.EmailSendResult;
import com.nivya.email.provider.EmailProvider;
import com.nivya.email.provider.EmailProviderFactory;
import com.nivya.family.entity.Family;
import com.nivya.family.entity.FamilyMember;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.family.repository.FamilyRepository;
import com.nivya.role.RoleType;
import com.nivya.user.dto.AccountDeletionStatusDto;
import com.nivya.user.dto.DeleteAccountRequest;
import com.nivya.user.dto.RequestChildApprovalResponse;
import com.nivya.user.entity.DeletionApprovalCode;
import com.nivya.user.entity.User;
import com.nivya.user.repository.DeletionApprovalCodeRepository;
import com.nivya.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long CODE_TTL_MINUTES = 15;

    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final DeletionApprovalCodeRepository approvalCodeRepository;
    private final DeviceRepository deviceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final com.nivya.email.repository.EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final com.nivya.email.repository.EmailPreferenceRepository emailPreferenceRepository;
    private final com.nivya.session.repository.DeviceSessionRepository deviceSessionRepository;
    private final com.nivya.pairing.repository.PairingRequestRepository pairingRequestRepository;
    private final com.nivya.consent.repository.ConsentRepository consentRepository;
    private final com.nivya.convocation.repository.ConvocationMessageRepository convocationMessageRepository;
    private final com.nivya.email.service.EmailService emailService;
    private final EmailProviderFactory emailProviderFactory;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final TransactionTemplate transactionTemplate;

    public AccountDeletionService(
            UserRepository userRepository,
            FamilyRepository familyRepository,
            FamilyMemberRepository familyMemberRepository,
            DeletionApprovalCodeRepository approvalCodeRepository,
            DeviceRepository deviceRepository,
            RefreshTokenRepository refreshTokenRepository,
            com.nivya.email.repository.EmailVerificationCodeRepository emailVerificationCodeRepository,
            com.nivya.email.repository.EmailPreferenceRepository emailPreferenceRepository,
            com.nivya.session.repository.DeviceSessionRepository deviceSessionRepository,
            com.nivya.pairing.repository.PairingRequestRepository pairingRequestRepository,
            com.nivya.consent.repository.ConsentRepository consentRepository,
            com.nivya.convocation.repository.ConvocationMessageRepository convocationMessageRepository,
            com.nivya.email.service.EmailService emailService,
            EmailProviderFactory emailProviderFactory,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            PlatformTransactionManager transactionManager) {
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.approvalCodeRepository = approvalCodeRepository;
        this.deviceRepository = deviceRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailVerificationCodeRepository = emailVerificationCodeRepository;
        this.emailPreferenceRepository = emailPreferenceRepository;
        this.deviceSessionRepository = deviceSessionRepository;
        this.pairingRequestRepository = pairingRequestRepository;
        this.consentRepository = consentRepository;
        this.convocationMessageRepository = convocationMessageRepository;
        this.emailService = emailService;
        this.emailProviderFactory = emailProviderFactory;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public AccountDeletionStatusDto getDeletionStatus(User user) {
        if (user.getRole() == RoleType.PARENT) {
            return new AccountDeletionStatusDto(
                    RoleType.PARENT,
                    false,
                    false,
                    null,
                    "Parent account deletion requires password verification."
            );
        }

        // Check if child is connected to a parent
        Optional<User> connectedParent = findConnectedParent(user.getId());
        if (connectedParent.isPresent()) {
            User parent = connectedParent.get();
            String maskedEmail = maskEmail(parent.getEmail());

            Optional<DeletionApprovalCode> latestCodeOpt = approvalCodeRepository
                    .findFirstByChildUserIdOrderByCreatedAtDesc(user.getId());

            String deliveryStatus = "IDLE";
            boolean hasPendingCode = false;
            Long expiresInSeconds = null;

            if (latestCodeOpt.isPresent()) {
                DeletionApprovalCode latest = latestCodeOpt.get();
                if ("PENDING".equals(latest.getStatus()) && !latest.isExpired() && !latest.isExhausted() && !latest.isUsed()) {
                    deliveryStatus = "DELIVERED";
                    hasPendingCode = true;
                    expiresInSeconds = Math.max(0, Duration.between(Instant.now(), latest.getExpiresAt()).getSeconds());
                } else if ("DISPATCHING".equals(latest.getStatus()) && !latest.isExpired()) {
                    deliveryStatus = "DISPATCHING";
                } else if ("REVOKED".equals(latest.getStatus())) {
                    deliveryStatus = "FAILED";
                }
            }

            return new AccountDeletionStatusDto(
                    RoleType.CHILD,
                    true,
                    true,
                    maskedEmail,
                    "Child account is connected to a parent. Deletion requires parent approval.",
                    hasPendingCode,
                    expiresInSeconds,
                    deliveryStatus
            );
        } else {
            return new AccountDeletionStatusDto(
                    RoleType.CHILD,
                    true,
                    false,
                    null,
                    "Child account is not currently connected to a parent. Deletion requires password verification."
            );
        }
    }

    public RequestChildApprovalResponse requestChildDeletionApproval(User child) {
        if (child.getRole() != RoleType.CHILD) {
            throw new IllegalArgumentException("Only child accounts can request parent deletion approval.");
        }

        // 1. Short transaction to find parent, revoke previous codes, generate new code, and log audit event
        record ChildApprovalContext(
                Long codeRecordId,
                String rawCode,
                Long parentId,
                String parentEmail,
                Long childId,
                String childName,
                String childEmail
        ) {}

        ChildApprovalContext ctx = transactionTemplate.execute(status -> {
            User parent = findConnectedParent(child.getId())
                    .orElseThrow(() -> new IllegalStateException("Child account is not connected to a parent. Cannot request parent approval."));

            User childUser = userRepository.findById(child.getId()).orElse(child);

            // Invalidate any previously pending or dispatching codes for this child
            List<DeletionApprovalCode> oldCodes = new ArrayList<>();
            oldCodes.addAll(approvalCodeRepository.findAllByChildUserIdAndStatus(child.getId(), "PENDING"));
            oldCodes.addAll(approvalCodeRepository.findAllByChildUserIdAndStatus(child.getId(), "DISPATCHING"));
            for (DeletionApprovalCode oldCode : oldCodes) {
                oldCode.revoke();
            }
            if (!oldCodes.isEmpty()) {
                approvalCodeRepository.saveAll(oldCodes);
            }

            // Generate 6-digit cryptographically secure code
            int codeInt = 100000 + RANDOM.nextInt(900000);
            String rawCode = String.valueOf(codeInt);
            String codeHash = sha256(rawCode);
            Instant expiresAt = Instant.now().plus(Duration.ofMinutes(CODE_TTL_MINUTES));

            // Persist initially with DISPATCHING status - code is NOT usable until email confirms delivery
            DeletionApprovalCode codeRecord = new DeletionApprovalCode(child.getId(), parent.getId(), codeHash, expiresAt);
            codeRecord.setStatus("DISPATCHING");
            codeRecord = approvalCodeRepository.save(codeRecord);

            auditService.logEvent(child.getId(), "CHILD_DELETION_APPROVAL_REQUESTED",
                    "Child requested account deletion approval. Code dispatched to parent.", "SYSTEM");

            return new ChildApprovalContext(
                    codeRecord.getId(),
                    rawCode,
                    parent.getId(),
                    parent.getEmail(),
                    childUser.getId(),
                    childUser.getName(),
                    childUser.getEmail()
            );
        });

        if (ctx == null) {
            throw new IllegalStateException("Failed to generate deletion approval code.");
        }

        // 2. Transaction is now committed. All locks on deletion_approval_codes and users are released.
        // 3. ONLY AFTER COMMIT: Dispatch notification email to connected parent outside any DB transaction
        try {
            EmailSendResult result = emailService.sendChildDeletionApprovalEmail(
                    ctx.parentId(), ctx.parentEmail(), ctx.childId(), ctx.childName(), ctx.childEmail(), ctx.rawCode()
            );
            if (result.isSuccess()) {
                // Phase 3A: SUCCESS - Short DB transaction: Activate code from DISPATCHING to PENDING
                transactionTemplate.executeWithoutResult(status -> {
                    approvalCodeRepository.findById(ctx.codeRecordId()).ifPresent(code -> {
                        if ("DISPATCHING".equals(code.getStatus())) {
                            code.setStatus("PENDING");
                            approvalCodeRepository.save(code);
                        }
                    });
                });
            } else {
                log.error("Failed to deliver parent deletion approval email to {}: provider={}, error={}",
                        maskEmail(ctx.parentEmail()), result.getProvider(), result.getErrorMessage());
                // Phase 3B: FAILURE - Short DB transaction: Revoke code
                transactionTemplate.executeWithoutResult(status -> {
                    approvalCodeRepository.findById(ctx.codeRecordId()).ifPresent(code -> {
                        code.revoke();
                        approvalCodeRepository.save(code);
                    });
                    auditService.logEvent(child.getId(), "CHILD_DELETION_EMAIL_FAILED",
                            "Failed to send parent approval email. Code revoked.", "SYSTEM");
                });
                throw new IllegalStateException("Failed to send approval code to parent email: " + result.getErrorMessage());
            }
        } catch (Exception e) {
            // Email delivery threw exception: ensure code is revoked in a short transaction
            transactionTemplate.executeWithoutResult(status -> {
                approvalCodeRepository.findById(ctx.codeRecordId()).ifPresent(code -> {
                    if (!"REVOKED".equals(code.getStatus())) {
                        code.revoke();
                        approvalCodeRepository.save(code);
                    }
                });
                auditService.logEvent(child.getId(), "CHILD_DELETION_EMAIL_FAILED",
                        "Failed to send parent approval email. Code revoked.", "SYSTEM");
            });
            throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
        }

        String maskedEmail = maskEmail(ctx.parentEmail());
        return new RequestChildApprovalResponse(
                "Approval code sent to your connected parent (" + maskedEmail + ").",
                maskedEmail,
                (int) CODE_TTL_MINUTES
        );
    }

    @Transactional
    public boolean verifyChildApprovalCode(User child, String rawCode) {
        if (rawCode == null || rawCode.trim().length() != 6) {
            return false;
        }

        Optional<DeletionApprovalCode> codeOpt = approvalCodeRepository
                .findFirstByChildUserIdAndStatusOrderByCreatedAtDesc(child.getId(), "PENDING");

        if (codeOpt.isEmpty()) {
            auditService.logEvent(child.getId(), "CHILD_DELETION_CODE_FAILED", "No active approval code found", "SYSTEM");
            return false;
        }

        DeletionApprovalCode codeRecord = codeOpt.get();
        if (codeRecord.isExpired() || codeRecord.isExhausted() || codeRecord.isUsed()) {
            codeRecord.setStatus("EXPIRED");
            approvalCodeRepository.save(codeRecord);
            auditService.logEvent(child.getId(), "CHILD_DELETION_CODE_REJECTED", "Approval code expired or exhausted", "SYSTEM");
            return false;
        }

        codeRecord.incrementAttempts();

        String inputHash = sha256(rawCode.trim());
        if (!codeRecord.getCodeHash().equals(inputHash)) {
            approvalCodeRepository.save(codeRecord);
            auditService.logEvent(child.getId(), "CHILD_DELETION_CODE_MISMATCH", "Invalid approval code attempt", "SYSTEM");
            return false;
        }

        approvalCodeRepository.save(codeRecord);
        auditService.logEvent(child.getId(), "CHILD_DELETION_CODE_VERIFIED", "Approval code verified successfully", "SYSTEM");
        return true;
    }

    @Transactional
    public void executeAccountDeletion(User user, DeleteAccountRequest request) {
        Long userId = user.getId();
        RoleType role = user.getRole();
        String userEmail = user.getEmail();
        String userName = user.getName();

        // Capture connected parent details BEFORE any unlinking or deletion occurs
        Optional<User> connectedParentOpt = (role == RoleType.CHILD) ? findConnectedParent(userId) : Optional.empty();
        String connectedParentEmail = connectedParentOpt.map(User::getEmail).orElse(null);
        String connectedParentName = connectedParentOpt.map(User::getName).orElse(null);
        boolean isConnectedChild = (role == RoleType.CHILD && connectedParentEmail != null);

        // 1. Authorize deletion
        if (role == RoleType.PARENT) {
            if (request.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                auditService.logEvent(userId, "ACCOUNT_DELETION_UNAUTHORIZED", "Invalid password provided for account deletion", "SYSTEM");
                throw new IllegalArgumentException("Invalid password. Identity confirmation failed.");
            }
        } else {
            // Child role
            if (isConnectedChild) {
                // Connected Child requires valid parent approval code
                if (request.getApprovalCode() == null || request.getApprovalCode().trim().length() != 6) {
                    throw new IllegalArgumentException("Parent approval code is required for connected child account deletion.");
                }

                Optional<DeletionApprovalCode> codeOpt = approvalCodeRepository
                        .findFirstByChildUserIdAndStatusOrderByCreatedAtDesc(userId, "PENDING");

                if (codeOpt.isEmpty()) {
                    throw new IllegalArgumentException("No active deletion approval code found. Please request parent approval.");
                }

                DeletionApprovalCode codeRecord = codeOpt.get();
                if (codeRecord.isExpired() || codeRecord.isExhausted() || codeRecord.isUsed()) {
                    throw new IllegalArgumentException("Approval code has expired or was exhausted. Please request a new code.");
                }

                codeRecord.incrementAttempts();
                if (!codeRecord.getCodeHash().equals(sha256(request.getApprovalCode().trim()))) {
                    approvalCodeRepository.save(codeRecord);
                    auditService.logEvent(userId, "CHILD_DELETION_CODE_MISMATCH", "Invalid approval code at final deletion", "SYSTEM");
                    throw new IllegalArgumentException("Invalid parent approval code.");
                }

                codeRecord.markUsed();
                approvalCodeRepository.save(codeRecord);
            } else {
                // Disconnected Child requires password
                if (request.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                    auditService.logEvent(userId, "ACCOUNT_DELETION_UNAUTHORIZED", "Invalid password provided for disconnected child deletion", "SYSTEM");
                    throw new IllegalArgumentException("Invalid password. Identity confirmation failed.");
                }
            }
        }

        // 2. Perform safe, non-cascading data cleanup
        log.info("Executing permanent account deletion for user ID: {}, email: {}, role: {}", userId, userEmail, role);

        // Revoke all tokens
        refreshTokenRepository.revokeAllUserTokens(userId, Instant.now());

        // Handle family memberships cleanly without deleting other family members
        Optional<FamilyMember> memberOpt = familyMemberRepository.findByUserId(userId);
        if (memberOpt.isPresent()) {
            FamilyMember currentMember = memberOpt.get();
            Long familyId = currentMember.getFamily().getId();
            List<FamilyMember> allMembers = familyMemberRepository.findByFamilyId(familyId);

            Optional<Family> familyOpt = familyRepository.findById(familyId);
            if (familyOpt.isPresent()) {
                Family family = familyOpt.get();
                // Check if other members exist in the family
                List<FamilyMember> remainingMembers = allMembers.stream()
                        .filter(m -> !m.getUser().getId().equals(userId))
                        .toList();

                if (remainingMembers.isEmpty()) {
                    // User was the sole member -> delete the family
                    familyMemberRepository.delete(currentMember);
                    familyRepository.delete(family);
                } else {
                    // Other members exist!
                    // If deleting user was the creator, reassign createdBy to a remaining user to avoid cascade delete
                    if (family.getCreatedBy() != null && family.getCreatedBy().getId().equals(userId)) {
                        User newOwner = remainingMembers.get(0).getUser();
                        if (newOwner != null) {
                            family.setCreatedBy(newOwner);
                            familyRepository.save(family);
                        }
                    }
                    familyMemberRepository.delete(currentMember);
                }
            } else {
                familyMemberRepository.delete(currentMember);
            }
        }

        // Also reassign or clean up any other families created by this user
        List<Family> createdFamilies = familyRepository.findAll().stream()
                .filter(f -> f.getCreatedBy() != null && f.getCreatedBy().getId().equals(userId))
                .toList();
        for (Family f : createdFamilies) {
            List<FamilyMember> remaining = familyMemberRepository.findByFamilyId(f.getId()).stream()
                    .filter(m -> !m.getUser().getId().equals(userId))
                    .toList();
            if (remaining.isEmpty()) {
                familyRepository.delete(f);
            } else {
                User nextOwner = remaining.get(0).getUser();
                if (nextOwner != null) {
                    f.setCreatedBy(nextOwner);
                    familyRepository.save(f);
                }
            }
        }

        // Clean up user's enrolled devices and status
        List<Device> userDevices = deviceRepository.findByUserId(userId);
        if (!userDevices.isEmpty()) {
            deviceRepository.deleteAll(userDevices);
        }

        // Clean up user records in dependent tables to ensure referential integrity
        approvalCodeRepository.deleteAllByChildUserIdOrParentUserId(userId);
        refreshTokenRepository.deleteAllByUserId(userId);
        emailVerificationCodeRepository.deleteAllByUserIdOrEmail(userId, userEmail);
        emailPreferenceRepository.deleteByUserId(userId);
        deviceSessionRepository.deleteByUserId(userId);
        pairingRequestRepository.deleteAllByUserId(userId);
        consentRepository.deleteByUserId(userId);
        convocationMessageRepository.deleteAllByUserId(userId);

        // Delete user account permanently
        try {
            userRepository.delete(user);
            userRepository.flush();
        } catch (Exception e) {
            log.error("CRITICAL: Failed to delete user {}: {} | root cause: {}", userId, e.getMessage(), e.getCause());
            throw e;
        }

        auditService.logEvent(null, "ACCOUNT_DELETED",
                "Permanently deleted account: " + userEmail + " (Role: " + role + ")", "SYSTEM");
        log.info("Successfully deleted user ID: {}, email: {}", userId, userEmail);

        // 3. Post-Commit Asynchronous Email Dispatch
        // Guarantees notification emails are ONLY dispatched upon successful database transaction commit.
        // If deletion fails or rolls back, this callback is never invoked.
        Runnable emailDispatcher = () -> {
            try {
                if (role == RoleType.PARENT) {
                    emailService.sendParentAccountDeletionConfirmationAsync(userEmail, userName);
                } else if (role == RoleType.CHILD) {
                    emailService.sendChildAccountDeletionConfirmationAsync(userEmail, userName);
                    if (isConnectedChild && connectedParentEmail != null && !connectedParentEmail.isBlank()) {
                        emailService.sendParentNotificationOfChildDeletionAsync(
                                connectedParentEmail, connectedParentName, userName, userEmail);
                    }
                }
            } catch (Exception e) {
                log.error("Non-blocking error dispatching post-deletion email notifications for {}: {}", userEmail, e.getMessage());
            }
        };

        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            emailDispatcher.run();
                        }
                    }
            );
        } else {
            emailDispatcher.run();
        }
    }

    private Optional<User> findConnectedParent(Long childUserId) {
        Optional<FamilyMember> childMemberOpt = familyMemberRepository.findByUserId(childUserId);
        if (childMemberOpt.isEmpty()) {
            return Optional.empty();
        }

        Long familyId = childMemberOpt.get().getFamily().getId();
        List<FamilyMember> members = familyMemberRepository.findByFamilyId(familyId);

        for (FamilyMember member : members) {
            if (member.getMemberRole() == RoleType.PARENT && !member.getUser().getId().equals(childUserId)) {
                return userRepository.findById(member.getUser().getId());
            }
        }
        return Optional.empty();
    }

    private void dispatchParentApprovalEmail(User parent, User child, String rawCode) {
        EmailSendResult result = emailService.sendChildDeletionApprovalEmail(parent, child, rawCode);
        if (!result.isSuccess()) {
            log.error("Failed to deliver parent deletion approval email to {}: provider={}, error={}",
                    maskEmail(parent.getEmail()), result.getProvider(), result.getErrorMessage());
            throw new IllegalStateException("Failed to send approval code to parent email: " + result.getErrorMessage());
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "p***@example.com";
        }
        int atIdx = email.indexOf('@');
        String namePart = email.substring(0, atIdx);
        String domainPart = email.substring(atIdx);
        if (namePart.length() <= 2) {
            return namePart.charAt(0) + "***" + domainPart;
        }
        return namePart.substring(0, 2) + "***" + domainPart;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
