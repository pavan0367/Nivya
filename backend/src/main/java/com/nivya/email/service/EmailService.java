package com.nivya.email.service;

import com.nivya.audit.service.AuditService;
import com.nivya.email.dto.*;
import com.nivya.email.entity.*;
import com.nivya.email.provider.EmailProvider;
import com.nivya.email.provider.EmailProviderFactory;
import com.nivya.email.repository.EmailNotificationRepository;
import com.nivya.email.repository.EmailPreferenceRepository;
import com.nivya.email.repository.EmailVerificationCodeRepository;
import com.nivya.user.entity.User;
import com.nivya.user.entity.UserStatus;
import com.nivya.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long VERIFICATION_TTL_MINUTES = 15;

    private final EmailProviderFactory providerFactory;
    private final EmailNotificationRepository notificationRepository;
    private final EmailPreferenceRepository preferenceRepository;
    private final EmailVerificationCodeRepository verificationCodeRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public EmailService(
            EmailProviderFactory providerFactory,
            EmailNotificationRepository notificationRepository,
            EmailPreferenceRepository preferenceRepository,
            EmailVerificationCodeRepository verificationCodeRepository,
            UserRepository userRepository,
            AuditService auditService) {
        this.providerFactory = providerFactory;
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // =========================================================================
    // 1. EMAIL VERIFICATION CODES
    // =========================================================================

    @Transactional
    public void generateVerificationCode(String email, String purpose, User user) {
        String normalizedEmail = email.toLowerCase().trim();

        // If user not provided (e.g. unauthenticated resend), associate existing account if registered
        if (user == null) {
            user = userRepository.findByEmail(normalizedEmail).orElse(null);
        }

        // Invalidate previous active unused codes for this email and purpose so they cannot be reused
        List<EmailVerificationCode> existingCodes = verificationCodeRepository
                .findAllByEmailAndPurposeAndUsedAtIsNull(normalizedEmail, purpose);
        for (EmailVerificationCode oldCode : existingCodes) {
            oldCode.markUsed();
        }
        if (!existingCodes.isEmpty()) {
            verificationCodeRepository.saveAll(existingCodes);
        }

        // 1. Generate secure 6-digit verification code
        int codeInt = 100000 + RANDOM.nextInt(900000);
        String plaintextCode = String.valueOf(codeInt);

        // 2. Hash code with SHA-256 for secure storage at rest
        String codeHash = sha256(plaintextCode);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(VERIFICATION_TTL_MINUTES));

        EmailVerificationCode verificationCode = new EmailVerificationCode(user, normalizedEmail, codeHash, purpose, expiresAt);
        verificationCodeRepository.save(verificationCode);

        auditService.logEvent(user != null ? user.getId() : null, "EMAIL_VERIFICATION_CODE_GENERATED",
                "Verification code generated for " + normalizedEmail + " (" + purpose + ")", "SYSTEM");

        // 3. Dispatch email asynchronously (NEVER log the plaintext code)
        String subject = "Nivya - Email Verification";
        String bodyText = "Nivya\nEmail Verification\n\nYour verification code:\n" + plaintextCode +
                "\n\nThis code expires in " + VERIFICATION_TTL_MINUTES + " minutes.";

        String bodyHtml = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica, Arial, sans-serif; background-color: #0F172A; color: #F8FAFC; padding: 32px 16px; margin: 0;'>" +
                "<div style='max-width: 480px; margin: 0 auto; background: #1E293B; border-radius: 12px; border: 1px solid #334155; padding: 32px; box-shadow: 0 8px 24px rgba(0,0,0,0.3);'>" +
                "<div style='margin-bottom: 24px;'><h1 style='color: #6366F1; margin: 0; font-size: 24px; font-weight: 800; letter-spacing: 0.5px;'>Nivya</h1>" +
                "<h2 style='color: #E2E8F0; margin: 8px 0 0 0; font-size: 18px; font-weight: 600;'>Email Verification</h2></div>" +
                "<p style='color: #94A3B8; font-size: 14px; margin: 16px 0 8px 0;'>Your verification code:</p>" +
                "<div style='background: #0F172A; border: 1px solid #4F46E5; border-radius: 8px; padding: 18px; text-align: center; margin: 16px 0;'>" +
                "<span style='font-family: monospace; font-size: 32px; font-weight: 700; letter-spacing: 6px; color: #818CF8; display: inline-block;'>" + plaintextCode + "</span>" +
                "</div>" +
                "<p style='color: #CBD5E1; font-size: 13px; margin: 16px 0 0 0;'>This code expires in " + VERIFICATION_TTL_MINUTES + " minutes.</p>" +
                "</div></body></html>";

        String idempotencyKey = "verify-" + normalizedEmail + "-" + System.currentTimeMillis();
        dispatchEmailAsync(user, normalizedEmail, "VERIFICATION", subject, bodyHtml, bodyText, idempotencyKey);
    }

    @Transactional
    public boolean verifyCode(String email, String plaintextCode, String purpose) {
        String normalizedEmail = email.toLowerCase().trim();
        Optional<EmailVerificationCode> optCode = verificationCodeRepository
                .findFirstByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(normalizedEmail, purpose);

        if (optCode.isEmpty()) {
            auditService.logEvent(null, "EMAIL_VERIFY_FAILED", "No active verification code found for " + normalizedEmail, "SYSTEM");
            return false;
        }

        EmailVerificationCode codeRecord = optCode.get();
        if (codeRecord.isExpired() || codeRecord.isExhausted() || codeRecord.isUsed()) {
            auditService.logEvent(codeRecord.getUser() != null ? codeRecord.getUser().getId() : null,
                    "EMAIL_VERIFY_REJECTED", "Verification code expired or exhausted for " + normalizedEmail, "SYSTEM");
            return false;
        }

        codeRecord.incrementAttempts();

        String inputHash = sha256(plaintextCode.trim());
        if (!codeRecord.getCodeHash().equals(inputHash)) {
            verificationCodeRepository.save(codeRecord);
            auditService.logEvent(codeRecord.getUser() != null ? codeRecord.getUser().getId() : null,
                    "EMAIL_VERIFY_MISMATCH", "Invalid verification code attempt for " + normalizedEmail, "SYSTEM");
            return false;
        }

        codeRecord.markUsed();
        verificationCodeRepository.save(codeRecord);

        // Activate user account if in PENDING state
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            if (user.getStatus() == UserStatus.PENDING) {
                user.setStatus(UserStatus.ACTIVE);
                userRepository.save(user);
                auditService.logEvent(user.getId(), "USER_STATUS_ACTIVATED",
                        "Account activated after email verification for " + normalizedEmail, "SYSTEM");
                log.info("Activated account for user ID: {}, email: {}", user.getId(), normalizedEmail);
            }
        });

        auditService.logEvent(codeRecord.getUser() != null ? codeRecord.getUser().getId() : null,
                "EMAIL_VERIFY_SUCCESS", "Verification code verified successfully for " + normalizedEmail, "SYSTEM");
        return true;
    }

    // =========================================================================
    // 2. SECURITY NOTIFICATIONS (LOGIN, LOGOUT, NEW DEVICE)
    // =========================================================================

    @Async("taskExecutor")
    public void sendLoginNotificationAsync(User user, String deviceName, String platform, String osVersion,
                                           String appVersion, String ipAddress, String location) {
        try {
            EmailPreference pref = getOrCreatePreference(user);
            if (!pref.isLoginAlertsEnabled()) {
                log.debug("Login alerts disabled for user {}", user.getEmail());
                return;
            }

            String timestampStr = formatTimestamp(Instant.now());
            String subject = "Nivya Security Alert: New Login to Your Account";
            String bodyText = String.format(
                    "Hello %s,\n\nA new login to your Nivya account occurred.\n\n" +
                            "Date/Time: %s\nDevice: %s\nPlatform: %s\nOS: %s\nApp Version: %s\nApprox Location: %s\nIP: %s\n\n" +
                            "If this was not you, please immediately secure your account.\n\nNivya Security",
                    user.getName(), timestampStr, deviceName != null ? deviceName : "Unknown Device",
                    platform != null ? platform : "Unknown", osVersion != null ? osVersion : "Unknown",
                    appVersion != null ? appVersion : "1.0.0", location != null ? location : "Unknown Location",
                    ipAddress != null ? ipAddress : "Unknown"
            );

            String bodyHtml = String.format(
                    "<html><body><h3>Nivya Security Alert: New Login</h3>" +
                            "<p>A login to your Nivya account was detected:</p>" +
                            "<ul><li><strong>Time:</strong> %s</li>" +
                            "<li><strong>Device:</strong> %s (%s)</li>" +
                            "<li><strong>Approximate Location:</strong> %s</li>" +
                            "<li><strong>IP Address:</strong> %s</li></ul>" +
                            "<p>If this was not you, please log out of all sessions immediately.</p></body></html>",
                    timestampStr, deviceName != null ? deviceName : "Unknown Device",
                    platform != null ? platform : "Unknown", location != null ? location : "Unknown Location",
                    ipAddress != null ? ipAddress : "Unknown"
            );

            String idempotencyKey = "login-" + user.getId() + "-" + System.currentTimeMillis();
            dispatchEmailSync(user, user.getEmail(), "LOGIN", subject, bodyHtml, bodyText, idempotencyKey);
        } catch (Exception e) {
            log.error("Failed to send login notification to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void sendLogoutNotificationAsync(User user, String ipAddress) {
        try {
            String timestampStr = formatTimestamp(Instant.now());
            String subject = "Nivya Account Notice: Session Logout";
            String bodyText = String.format(
                    "Hello %s,\n\nYour Nivya account was logged out on %s from IP %s.\n\nNivya Security",
                    user.getName(), timestampStr, ipAddress != null ? ipAddress : "Unknown"
            );
            String bodyHtml = String.format(
                    "<html><body><p>Your Nivya account session was logged out at %s.</p></body></html>",
                    timestampStr
            );

            String idempotencyKey = "logout-" + user.getId() + "-" + System.currentTimeMillis();
            dispatchEmailSync(user, user.getEmail(), "LOGOUT", subject, bodyHtml, bodyText, idempotencyKey);
        } catch (Exception e) {
            // Email failure must NEVER affect logout
            log.warn("Logout notification failed non-blockingly: {}", e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void sendNewDeviceLoginNotificationAsync(User user, String deviceName, String platform, String osVersion,
                                                    String appVersion, String ipAddress, String location) {
        try {
            EmailPreference pref = getOrCreatePreference(user);
            if (!pref.isNewDeviceAlertsEnabled()) {
                return;
            }

            String timestampStr = formatTimestamp(Instant.now());
            String subject = "Nivya Security Alert: Unrecognized Device Login";
            String bodyText = String.format(
                    "Hello %s,\n\nA new unrecognized device just logged into your Nivya account.\n\n" +
                            "Device: %s\nPlatform: %s\nTime: %s\nLocation: %s\nIP: %s\n\n" +
                            "If you do not recognize this device, please revoke its access from the Security dashboard.\n\nNivya Security",
                    user.getName(), deviceName != null ? deviceName : "New Device",
                    platform != null ? platform : "Unknown", timestampStr,
                    location != null ? location : "Unknown Location", ipAddress != null ? ipAddress : "Unknown"
            );

            String bodyHtml = String.format(
                    "<html><body><h3 style='color: #EF4444;'>Security Alert: Unrecognized Device</h3>" +
                            "<p>A new device has logged into your Nivya account:</p>" +
                            "<ul><li><strong>Device:</strong> %s</li>" +
                            "<li><strong>Platform:</strong> %s</li>" +
                            "<li><strong>Time:</strong> %s</li>" +
                            "<li><strong>Location:</strong> %s</li></ul>" +
                            "<p>If this was not you, please access your Nivya Settings to revoke this session.</p></body></html>",
                    deviceName != null ? deviceName : "New Device", platform != null ? platform : "Unknown",
                    timestampStr, location != null ? location : "Unknown Location"
            );

            String idempotencyKey = "new-device-" + user.getId() + "-" + System.currentTimeMillis();
            dispatchEmailSync(user, user.getEmail(), "NEW_DEVICE", subject, bodyHtml, bodyText, idempotencyKey);
        } catch (Exception e) {
            log.error("Failed to send new device login email: {}", e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void sendAppUpdateNotificationAsync(String version, String releaseNotes) {
        try {
            List<User> users = userRepository.findAll();
            for (User user : users) {
                EmailPreference pref = getOrCreatePreference(user);
                if (pref.isAppUpdatesEnabled()) {
                    String subject = "Nivya Update: Version " + version + " is Now Available";
                    String bodyText = "Hello " + user.getName() + ",\n\nA new version (" + version + ") of Nivya is now available.\n\n" +
                            releaseNotes + "\n\nTogether for a Safer Tomorrow,\nTeam Nivya";
                    String bodyHtml = "<html><body><h3>Nivya Update " + version + "</h3><p>" + releaseNotes + "</p></body></html>";

                    String idempotencyKey = "app-update-" + version + "-" + user.getId();
                    dispatchEmailSync(user, user.getEmail(), "APP_UPDATE", subject, bodyHtml, bodyText, idempotencyKey);
                }
            }
        } catch (Exception e) {
            log.error("Failed to broadcast app update email: {}", e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void sendSecurityNotificationAsync(User user, String title, String details) {
        try {
            String subject = "Nivya Security Notice: " + title;
            String bodyText = "Hello " + user.getName() + ",\n\n" + title + "\n\n" + details + "\n\nNivya Security";
            String bodyHtml = "<html><body><h3>" + title + "</h3><p>" + details + "</p></body></html>";

            String idempotencyKey = "sec-" + user.getId() + "-" + System.currentTimeMillis();
            dispatchEmailSync(user, user.getEmail(), "SECURITY", subject, bodyHtml, bodyText, idempotencyKey);
        } catch (Exception e) {
            log.error("Failed to send security email: {}", e.getMessage());
        }
    }

    // =========================================================================
    // 3. ACCOUNT DELETION NOTIFICATIONS
    // =========================================================================

    @Async("taskExecutor")
    public void sendParentAccountDeletionConfirmationAsync(String recipientEmail, String recipientName) {
        try {
            String timestampStr = formatTimestamp(Instant.now());
            String subject = "Nivya Account Permanently Deleted";
            String bodyText = String.format(
                    "Hello %s,\n\n" +
                    "Your Nivya parent administrator account (%s) has been permanently deleted.\n\n" +
                    "This action is complete and irreversible. All your personal credentials, session tokens, device registrations, and security settings have been permanently removed from our servers.\n\n" +
                    "Date/Time of Deletion: %s\n\n" +
                    "If you did not authorize this action, please contact Nivya Support immediately.\n\n" +
                    "Nivya Security Team",
                    recipientName != null && !recipientName.isBlank() ? recipientName : "Parent",
                    recipientEmail,
                    timestampStr
            );

            String bodyHtml = String.format(
                    "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>" +
                    "<body style='font-family: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica, Arial, sans-serif; background-color: #0F172A; color: #F8FAFC; padding: 32px 16px; margin: 0;'>" +
                    "<div style='max-width: 480px; margin: 0 auto; background: #1E293B; border-radius: 12px; border: 1px solid #334155; padding: 32px; box-shadow: 0 8px 24px rgba(0,0,0,0.3);'>" +
                    "<div style='margin-bottom: 20px;'><h1 style='color: #EF4444; margin: 0; font-size: 22px; font-weight: 800;'>Nivya</h1>" +
                    "<h2 style='color: #E2E8F0; margin: 6px 0 0 0; font-size: 17px; font-weight: 600;'>Account Permanently Deleted</h2></div>" +
                    "<p style='color: #94A3B8; font-size: 14px; line-height: 1.5; margin: 16px 0;'>" +
                    "Hello <strong style='color: #F8FAFC;'>%s</strong>," +
                    "</p>" +
                    "<p style='color: #94A3B8; font-size: 14px; line-height: 1.5; margin: 16px 0;'>" +
                    "Your Nivya parent account (<strong style='color: #F8FAFC;'>%s</strong>) has been permanently deleted. This action is complete and irreversible." +
                    "</p>" +
                    "<div style='background: #0F172A; border-left: 3px solid #EF4444; border-radius: 6px; padding: 14px 16px; margin: 16px 0;'>" +
                    "<p style='color: #CBD5E1; font-size: 13px; margin: 0;'>All personal credentials, device sessions, and configuration settings have been permanently removed from our servers.</p>" +
                    "</div>" +
                    "<p style='color: #64748B; font-size: 12px; margin: 16px 0 0 0;'>Date/Time of Deletion: %s</p>" +
                    "<p style='color: #64748B; font-size: 12px; margin: 8px 0 0 0;'>If you did not authorize this action, please contact Nivya Support immediately.</p>" +
                    "</div></body></html>",
                    recipientName != null && !recipientName.isBlank() ? recipientName : "Parent",
                    recipientEmail,
                    timestampStr
            );

            String idempotencyKey = "parent-del-conf-" + recipientEmail + "-" + System.currentTimeMillis();
            dispatchEmailSync(null, recipientEmail, "ACCOUNT_DELETED", subject, bodyHtml, bodyText, idempotencyKey);
        } catch (Exception e) {
            log.error("Failed to send parent account deletion confirmation to {}: {}", recipientEmail, e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void sendChildAccountDeletionConfirmationAsync(String recipientEmail, String recipientName) {
        try {
            String timestampStr = formatTimestamp(Instant.now());
            String subject = "Your Nivya Account Permanently Deleted";
            String bodyText = String.format(
                    "Hello %s,\n\n" +
                    "Your Nivya account (%s) has been permanently deleted.\n\n" +
                    "This action is complete and irreversible. All your personal data, credentials, and active device sessions have been permanently removed from our servers.\n\n" +
                    "Date/Time of Deletion: %s\n\n" +
                    "Nivya Safety Team",
                    recipientName != null && !recipientName.isBlank() ? recipientName : "User",
                    recipientEmail,
                    timestampStr
            );

            String bodyHtml = String.format(
                    "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>" +
                    "<body style='font-family: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica, Arial, sans-serif; background-color: #0F172A; color: #F8FAFC; padding: 32px 16px; margin: 0;'>" +
                    "<div style='max-width: 480px; margin: 0 auto; background: #1E293B; border-radius: 12px; border: 1px solid #334155; padding: 32px; box-shadow: 0 8px 24px rgba(0,0,0,0.3);'>" +
                    "<div style='margin-bottom: 20px;'><h1 style='color: #EF4444; margin: 0; font-size: 22px; font-weight: 800;'>Nivya</h1>" +
                    "<h2 style='color: #E2E8F0; margin: 6px 0 0 0; font-size: 17px; font-weight: 600;'>Your Nivya Account Has Been Deleted</h2></div>" +
                    "<p style='color: #94A3B8; font-size: 14px; line-height: 1.5; margin: 16px 0;'>" +
                    "Hello <strong style='color: #F8FAFC;'>%s</strong>," +
                    "</p>" +
                    "<p style='color: #94A3B8; font-size: 14px; line-height: 1.5; margin: 16px 0;'>" +
                    "Your Nivya account (<strong style='color: #F8FAFC;'>%s</strong>) has been permanently deleted. This action is complete and irreversible." +
                    "</p>" +
                    "<div style='background: #0F172A; border-left: 3px solid #EF4444; border-radius: 6px; padding: 14px 16px; margin: 16px 0;'>" +
                    "<p style='color: #CBD5E1; font-size: 13px; margin: 0;'>All your personal data, credentials, and active device sessions have been permanently removed.</p>" +
                    "</div>" +
                    "<p style='color: #64748B; font-size: 12px; margin: 16px 0 0 0;'>Date/Time of Deletion: %s</p>" +
                    "</div></body></html>",
                    recipientName != null && !recipientName.isBlank() ? recipientName : "User",
                    recipientEmail,
                    timestampStr
            );

            String idempotencyKey = "child-del-conf-" + recipientEmail + "-" + System.currentTimeMillis();
            dispatchEmailSync(null, recipientEmail, "ACCOUNT_DELETED", subject, bodyHtml, bodyText, idempotencyKey);
        } catch (Exception e) {
            log.error("Failed to send child account deletion confirmation to {}: {}", recipientEmail, e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void sendParentNotificationOfChildDeletionAsync(String parentEmail, String parentName, String childName, String childEmail) {
        try {
            String timestampStr = formatTimestamp(Instant.now());
            String subject = "Your Child's Nivya Account Has Been Deleted";
            String bodyText = String.format(
                    "Hello %s,\n\n" +
                    "Your child's Nivya account has been permanently deleted.\n\n" +
                    "Account Details:\n" +
                    "Child Name: %s\n" +
                    "Child Email: %s\n" +
                    "Date/Time of Deletion: %s\n\n" +
                    "This action is complete and irreversible. Your child's account, devices, and profile credentials have been removed from your family unit.\n\n" +
                    "Your parent administrator account and other family members remain active and unaffected.\n\n" +
                    "Nivya Safety Team",
                    parentName != null && !parentName.isBlank() ? parentName : "Parent",
                    childName != null && !childName.isBlank() ? childName : "Child",
                    childEmail,
                    timestampStr
            );

            String bodyHtml = String.format(
                    "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>" +
                    "<body style='font-family: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica, Arial, sans-serif; background-color: #0F172A; color: #F8FAFC; padding: 32px 16px; margin: 0;'>" +
                    "<div style='max-width: 480px; margin: 0 auto; background: #1E293B; border-radius: 12px; border: 1px solid #334155; padding: 32px; box-shadow: 0 8px 24px rgba(0,0,0,0.3);'>" +
                    "<div style='margin-bottom: 20px;'><h1 style='color: #EF4444; margin: 0; font-size: 22px; font-weight: 800;'>Nivya</h1>" +
                    "<h2 style='color: #E2E8F0; margin: 6px 0 0 0; font-size: 17px; font-weight: 600;'>Child Account Deletion Notice</h2></div>" +
                    "<p style='color: #94A3B8; font-size: 14px; line-height: 1.5; margin: 16px 0;'>" +
                    "Hello <strong style='color: #F8FAFC;'>%s</strong>," +
                    "</p>" +
                    "<p style='color: #F8FAFC; font-size: 15px; font-weight: 600; line-height: 1.5; margin: 16px 0;'>" +
                    "Your child's Nivya account has been permanently deleted." +
                    "</p>" +
                    "<div style='background: #0F172A; border-left: 3px solid #EF4444; border-radius: 6px; padding: 14px 16px; margin: 16px 0;'>" +
                    "<p style='color: #CBD5E1; font-size: 13px; margin: 0 0 6px 0;'><strong>Child Name:</strong> %s</p>" +
                    "<p style='color: #CBD5E1; font-size: 13px; margin: 0 0 6px 0;'><strong>Child Email:</strong> %s</p>" +
                    "<p style='color: #CBD5E1; font-size: 13px; margin: 0;'><strong>Deletion Time:</strong> %s</p>" +
                    "</div>" +
                    "<p style='color: #94A3B8; font-size: 13px; line-height: 1.5; margin: 16px 0;'>" +
                    "This action is complete and irreversible. Your parent administrator account and other family members remain intact and unaffected." +
                    "</p>" +
                    "</div></body></html>",
                    parentName != null && !parentName.isBlank() ? parentName : "Parent",
                    childName != null && !childName.isBlank() ? childName : "Child",
                    childEmail,
                    timestampStr
            );

            String idempotencyKey = "parent-notify-child-del-" + childEmail + "-" + System.currentTimeMillis();
            dispatchEmailSync(null, parentEmail, "CHILD_ACCOUNT_DELETED_NOTICE", subject, bodyHtml, bodyText, idempotencyKey);
        } catch (Exception e) {
            log.error("Failed to send child deletion notice to parent {}: {}", maskEmail(parentEmail), e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EmailSendResult sendChildDeletionApprovalEmail(User parent, User child, String rawCode) {
        String subject = "Nivya - Child Account Deletion Request";
        String bodyText = "Nivya\nChild Account Deletion Request\n\n" +
                "Your connected child (" + child.getName() + " - " + child.getEmail() + ") has requested to permanently delete their Nivya account.\n\n" +
                "To approve this deletion, provide them with this 6-digit approval code:\n" +
                rawCode + "\n\n" +
                "This code expires in " + VERIFICATION_TTL_MINUTES + " minutes.\n" +
                "If you did not approve this request, you can safely ignore this email and the child's account will remain active.";

        String bodyHtml = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica, Arial, sans-serif; background-color: #0F172A; color: #F8FAFC; padding: 32px 16px; margin: 0;'>" +
                "<div style='max-width: 500px; margin: 0 auto; background: #1E293B; border-radius: 12px; border: 1px solid #334155; padding: 32px; box-shadow: 0 8px 24px rgba(0,0,0,0.3);'>" +
                "<div style='margin-bottom: 20px;'><h1 style='color: #EF4444; margin: 0; font-size: 22px; font-weight: 800;'>Nivya</h1>" +
                "<h2 style='color: #E2E8F0; margin: 6px 0 0 0; font-size: 17px; font-weight: 600;'>Child Account Deletion Request</h2></div>" +
                "<p style='color: #94A3B8; font-size: 14px; line-height: 1.5; margin: 16px 0;'>" +
                "Your connected child <strong style='color: #F8FAFC;'>" + child.getName() + "</strong> (" + child.getEmail() + ") has requested to permanently delete their Nivya account." +
                "</p>" +
                "<p style='color: #94A3B8; font-size: 14px; margin: 16px 0 8px 0;'>To approve this deletion, provide them with this 6-digit approval code:</p>" +
                "<div style='background: #0F172A; border: 1px solid #EF4444; border-radius: 8px; padding: 18px; text-align: center; margin: 16px 0;'>" +
                "<span style='font-family: monospace; font-size: 32px; font-weight: 700; letter-spacing: 6px; color: #F87171; display: inline-block;'>" + rawCode + "</span>" +
                "</div>" +
                "<p style='color: #CBD5E1; font-size: 13px; margin: 16px 0 0 0;'>This code expires in <strong>" + VERIFICATION_TTL_MINUTES + " minutes</strong>.</p>" +
                "<p style='color: #64748B; font-size: 12px; margin: 12px 0 0 0;'>If you did not approve this request, ignore this email and your child's account will remain active.</p>" +
                "</div></body></html>";

        String idempotencyKey = "child-del-approval-" + child.getId() + "-" + System.currentTimeMillis();
        return dispatchEmailSync(parent, parent.getEmail(), "CHILD_DELETION_APPROVAL", subject, bodyHtml, bodyText, idempotencyKey);
    }

    // =========================================================================
    // 3. PREFERENCES
    // =========================================================================

    @Transactional(readOnly = true)
    public EmailPreferenceDto getPreferences(User user) {
        EmailPreference pref = getOrCreatePreference(user);
        return EmailPreferenceDto.fromEntity(pref);
    }

    @Transactional
    public EmailPreferenceDto updatePreferences(User user, EmailPreferenceDto dto) {
        EmailPreference pref = getOrCreatePreference(user);
        pref.setLoginAlertsEnabled(dto.isLoginAlertsEnabled());
        pref.setNewDeviceAlertsEnabled(dto.isNewDeviceAlertsEnabled());
        pref.setAppUpdatesEnabled(dto.isAppUpdatesEnabled());
        // Security-critical notifications remain protected and cannot be disabled
        pref.setSecurityCriticalEnabled(true);
        preferenceRepository.save(pref);

        auditService.logEvent(user.getId(), "EMAIL_PREFERENCES_UPDATED", "Updated email notification settings", "SYSTEM");
        return EmailPreferenceDto.fromEntity(pref);
    }

    // =========================================================================
    // 4. DISPATCH ENGINE (ASYNC RETRY + IDEMPOTENCY + LOGGING)
    // =========================================================================

    @Async("taskExecutor")
    public void dispatchEmailAsync(User user, String recipient, String type, String subject,
                                  String bodyHtml, String bodyText, String idempotencyKey) {
        dispatchEmailSync(user, recipient, type, subject, bodyHtml, bodyText, idempotencyKey);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EmailSendResult dispatchEmailSync(User user, String recipient, String type, String subject,
                                             String bodyHtml, String bodyText, String idempotencyKey) {
        if (idempotencyKey != null && notificationRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            log.debug("Email with idempotency key {} already processed. Skipping duplicate.", idempotencyKey);
            return EmailSendResult.success("IDEMPOTENT", "already-processed");
        }

        EmailProvider provider = providerFactory.getProvider();
        EmailNotification notification = new EmailNotification(
                user, recipient, type, subject, provider.getProviderName(), idempotencyKey
        );
        notification = notificationRepository.save(notification);

        // Exponential backoff retry loop (max 3 attempts)
        int attempts = 0;
        boolean delivered = false;
        long backoffMs = 500;
        EmailSendResult lastResult = null;

        while (attempts < notification.getMaxAttempts() && !delivered) {
            attempts++;
            try {
                EmailSendResult result = provider.sendEmail(recipient, subject, bodyHtml, bodyText);
                lastResult = result;
                if (result.isSuccess()) {
                    notification.recordSuccess(result.getMessageId());
                    notificationRepository.save(notification);
                    delivered = true;
                    log.info("Email delivered: id={} to={} type={}", notification.getId(), maskEmail(recipient), type);
                    return result;
                } else {
                    notification.recordFailure(result.getErrorMessage());
                    notificationRepository.save(notification);
                    log.warn("Email attempt {}/{} failed for notification id={} to={} provider={}: {}",
                            attempts, notification.getMaxAttempts(), notification.getId(), maskEmail(recipient),
                            provider.getProviderName(), result.getErrorMessage());
                    if (attempts < notification.getMaxAttempts()) {
                        Thread.sleep(backoffMs);
                        backoffMs *= 2;
                    }
                }
            } catch (Exception e) {
                String sanitizedError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                lastResult = EmailSendResult.failure(provider.getProviderName(), sanitizedError);
                notification.recordFailure(sanitizedError);
                notificationRepository.save(notification);
                log.warn("Email attempt {}/{} exception for notification id={} to={}: {}",
                        attempts, notification.getMaxAttempts(), notification.getId(), maskEmail(recipient), sanitizedError);
                try {
                    if (attempts < notification.getMaxAttempts()) {
                        Thread.sleep(backoffMs);
                        backoffMs *= 2;
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return lastResult != null ? lastResult : EmailSendResult.failure(provider.getProviderName(), "Email delivery failed after max attempts");
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIdx = email.indexOf('@');
        String namePart = email.substring(0, atIdx);
        String domainPart = email.substring(atIdx);
        if (namePart.length() <= 2) {
            return namePart.charAt(0) + "***" + domainPart;
        }
        return namePart.substring(0, 2) + "***" + domainPart;
    }

    private EmailPreference getOrCreatePreference(User user) {
        return preferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> preferenceRepository.save(new EmailPreference(user)));
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }

    private String formatTimestamp(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                .withZone(ZoneId.of("UTC"))
                .format(instant);
    }
}
