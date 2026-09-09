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
import com.nivya.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
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
        String subject = "Your Nivya Verification Code";
        String bodyText = "Hello,\n\nYour Nivya verification code is: " + plaintextCode +
                "\n\nThis code expires in " + VERIFICATION_TTL_MINUTES + " minutes and can only be used once.\n" +
                "If you did not request this code, please ignore this email.\n\nTogether for a Safer Tomorrow,\nTeam Nivya";

        String bodyHtml = "<html><body style='font-family: Arial, sans-serif;'>" +
                "<h2>Nivya Email Verification</h2>" +
                "<p>Your single-use verification code is:</p>" +
                "<h1 style='letter-spacing: 4px; color: #6366F1;'>" + plaintextCode + "</h1>" +
                "<p>This code expires in <strong>" + VERIFICATION_TTL_MINUTES + " minutes</strong>.</p>" +
                "<p style='color: #64748B;'>If you did not request this, you can safely disregard this message.</p>" +
                "</body></html>";

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

    private void dispatchEmailSync(User user, String recipient, String type, String subject,
                                   String bodyHtml, String bodyText, String idempotencyKey) {
        if (idempotencyKey != null && notificationRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            log.debug("Email with idempotency key {} already processed. Skipping duplicate.", idempotencyKey);
            return;
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

        while (attempts < notification.getMaxAttempts() && !delivered) {
            attempts++;
            try {
                EmailSendResult result = provider.sendEmail(recipient, subject, bodyHtml, bodyText);
                if (result.isSuccess()) {
                    notification.recordSuccess(result.getMessageId());
                    notificationRepository.save(notification);
                    delivered = true;
                    log.info("Email delivered: id={} to={} type={}", notification.getId(), recipient, type);
                } else {
                    notification.recordFailure(result.getErrorMessage());
                    notificationRepository.save(notification);
                    Thread.sleep(backoffMs);
                    backoffMs *= 2;
                }
            } catch (Exception e) {
                notification.recordFailure(e.getMessage());
                notificationRepository.save(notification);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
                backoffMs *= 2;
            }
        }
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
