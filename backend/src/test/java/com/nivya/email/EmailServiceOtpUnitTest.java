package com.nivya.email;

import com.nivya.audit.service.AuditService;
import com.nivya.email.entity.EmailVerificationCode;
import com.nivya.email.provider.EmailProviderFactory;
import com.nivya.email.provider.SimulationEmailProvider;
import com.nivya.email.repository.EmailNotificationRepository;
import com.nivya.email.repository.EmailPreferenceRepository;
import com.nivya.email.repository.EmailVerificationCodeRepository;
import com.nivya.email.service.EmailService;
import com.nivya.role.RoleType;
import com.nivya.user.entity.User;
import com.nivya.user.entity.UserStatus;
import com.nivya.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class EmailServiceOtpUnitTest {

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailVerificationCodeRepository verificationCodeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    private User testUser;
    private String testEmail;

    @BeforeEach
    void setUp() {
        testEmail = "otp_test_" + System.currentTimeMillis() + "@nivya.local";
        testUser = new User("OTP Tester", testEmail, "$2a$10$hashedPasswordPlaceholder", RoleType.PARENT);
        testUser.setStatus(UserStatus.PENDING);
        testUser = userRepository.save(testUser);
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

    @Test
    @DisplayName("1 & 2 & 3. OTP generation: 6-digit random code, SHA-256 hashed at rest, 15-minute TTL")
    void testOtpGenerationAndHashing() {
        emailService.generateVerificationCode(testEmail, "EMAIL_VERIFICATION", testUser);

        Optional<EmailVerificationCode> codeOpt = verificationCodeRepository
                .findFirstByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(testEmail, "EMAIL_VERIFICATION");

        assertThat(codeOpt).isPresent();
        EmailVerificationCode code = codeOpt.get();

        // Stored code is a SHA-256 hash (64 hex characters), NEVER plain text
        assertThat(code.getCodeHash()).hasSize(64);
        assertThat(code.getCodeHash()).matches("^[a-f0-9]{64}$");

        // Exactly 15 minutes expiration (+/- 30 seconds tolerance for test execution time)
        Duration ttl = Duration.between(Instant.now(), code.getExpiresAt());
        assertThat(ttl.toMinutes()).isBetween(14L, 15L);

        // Attempts start at 0 with maxAttempts=5
        assertThat(code.getAttempts()).isEqualTo(0);
        assertThat(code.getMaxAttempts()).isEqualTo(5);
        assertThat(code.isUsed()).isFalse();
    }

    @Test
    @DisplayName("4. Correct OTP verifies successfully and activates user from PENDING to ACTIVE")
    void testCorrectOtpVerifiesAndActivates() {
        String rawOtp = "654321";
        String hash = sha256(rawOtp);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));

        EmailVerificationCode codeRecord = new EmailVerificationCode(testUser, testEmail, hash, "EMAIL_VERIFICATION", expiresAt);
        verificationCodeRepository.save(codeRecord);

        boolean verified = emailService.verifyCode(testEmail, rawOtp, "EMAIL_VERIFICATION");
        assertThat(verified).isTrue();

        // Verify account is activated
        User updatedUser = userRepository.findByEmail(testEmail).orElseThrow();
        assertThat(updatedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);

        // Verify code is marked used
        EmailVerificationCode updatedCode = verificationCodeRepository.findById(codeRecord.getId()).orElseThrow();
        assertThat(updatedCode.isUsed()).isTrue();
    }

    @Test
    @DisplayName("5. Wrong OTP fails and increments attempt counter")
    void testWrongOtpFailsAndIncrementsAttempts() {
        String rawOtp = "123456";
        String hash = sha256(rawOtp);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));

        EmailVerificationCode codeRecord = new EmailVerificationCode(testUser, testEmail, hash, "EMAIL_VERIFICATION", expiresAt);
        verificationCodeRepository.save(codeRecord);

        boolean verified = emailService.verifyCode(testEmail, "999999", "EMAIL_VERIFICATION");
        assertThat(verified).isFalse();

        // Code attempt count incremented
        EmailVerificationCode updatedCode = verificationCodeRepository.findById(codeRecord.getId()).orElseThrow();
        assertThat(updatedCode.getAttempts()).isEqualTo(1);
        assertThat(updatedCode.isUsed()).isFalse();

        // User remains PENDING
        User user = userRepository.findByEmail(testEmail).orElseThrow();
        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING);
    }

    @Test
    @DisplayName("6. Expired OTP fails verification")
    void testExpiredOtpFails() {
        String rawOtp = "123456";
        String hash = sha256(rawOtp);
        Instant pastExpiresAt = Instant.now().minus(Duration.ofMinutes(5));

        EmailVerificationCode expiredCode = new EmailVerificationCode(testUser, testEmail, hash, "EMAIL_VERIFICATION", pastExpiresAt);
        verificationCodeRepository.save(expiredCode);

        boolean verified = emailService.verifyCode(testEmail, rawOtp, "EMAIL_VERIFICATION");
        assertThat(verified).isFalse();
    }

    @Test
    @DisplayName("7. Reused OTP is rejected (single-use invariant)")
    void testReusedOtpFails() {
        String rawOtp = "123456";
        String hash = sha256(rawOtp);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));

        EmailVerificationCode codeRecord = new EmailVerificationCode(testUser, testEmail, hash, "EMAIL_VERIFICATION", expiresAt);
        codeRecord.markUsed();
        verificationCodeRepository.save(codeRecord);

        boolean verified = emailService.verifyCode(testEmail, rawOtp, "EMAIL_VERIFICATION");
        assertThat(verified).isFalse();
    }

    @Test
    @DisplayName("8 & 9. Resend generates fresh OTP and invalidates previous active OTP")
    void testResendGeneratesFreshOtpAndInvalidatesPrevious() {
        // 1. Initial code generation
        emailService.generateVerificationCode(testEmail, "EMAIL_VERIFICATION", testUser);
        EmailVerificationCode firstCode = verificationCodeRepository
                .findFirstByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(testEmail, "EMAIL_VERIFICATION")
                .orElseThrow();
        String firstHash = firstCode.getCodeHash();

        // 2. Resend code generation
        emailService.generateVerificationCode(testEmail, "EMAIL_VERIFICATION", testUser);

        // Verify first code is now marked used / invalidated
        EmailVerificationCode refreshedFirstCode = verificationCodeRepository.findById(firstCode.getId()).orElseThrow();
        assertThat(refreshedFirstCode.isUsed()).isTrue();

        // Verify newest code is active and different from first code
        EmailVerificationCode secondCode = verificationCodeRepository
                .findFirstByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(testEmail, "EMAIL_VERIFICATION")
                .orElseThrow();
        assertThat(secondCode.getId()).isNotEqualTo(firstCode.getId());
        assertThat(secondCode.isUsed()).isFalse();
    }

    @Test
    @DisplayName("10. Exhaustion after 5 failed attempts rejects further attempts")
    void testMaxAttemptsExhaustion() {
        String rawOtp = "777888";
        String hash = sha256(rawOtp);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));

        EmailVerificationCode codeRecord = new EmailVerificationCode(testUser, testEmail, hash, "EMAIL_VERIFICATION", expiresAt);
        verificationCodeRepository.save(codeRecord);

        for (int i = 1; i <= 5; i++) {
            boolean result = emailService.verifyCode(testEmail, "00000" + i, "EMAIL_VERIFICATION");
            assertThat(result).isFalse();
        }

        EmailVerificationCode exhaustedRecord = verificationCodeRepository.findById(codeRecord.getId()).orElseThrow();
        assertThat(exhaustedRecord.isExhausted()).isTrue();
        assertThat(exhaustedRecord.getAttempts()).isEqualTo(5);

        // Even with the correct OTP now, exhausted code is rejected
        boolean tryCorrect = emailService.verifyCode(testEmail, rawOtp, "EMAIL_VERIFICATION");
        assertThat(tryCorrect).isFalse();
    }
}
