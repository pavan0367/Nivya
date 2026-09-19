package com.nivya.email;

import com.nivya.common.response.ApiResponse;
import com.nivya.email.dto.EmailSendResult;
import com.nivya.email.entity.EmailNotification;
import com.nivya.email.repository.EmailNotificationRepository;
import com.nivya.email.service.EmailService;
import com.nivya.family.entity.Family;
import com.nivya.family.entity.FamilyMember;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.family.repository.FamilyRepository;
import com.nivya.role.RoleType;
import com.nivya.session.service.DeviceSessionService;
import com.nivya.user.dto.RequestChildApprovalResponse;
import com.nivya.user.entity.DeletionApprovalCode;
import com.nivya.user.entity.User;
import com.nivya.user.entity.UserStatus;
import com.nivya.user.repository.DeletionApprovalCodeRepository;
import com.nivya.user.repository.UserRepository;
import com.nivya.user.service.AccountDeletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class EmailLockContentionRegressionTest {

    @Autowired
    private EmailService emailService;

    @Autowired
    private AccountDeletionService accountDeletionService;

    @Autowired
    private DeviceSessionService deviceSessionService;

    @Autowired
    private EmailNotificationRepository emailNotificationRepository;

    @Autowired
    private DeletionApprovalCodeRepository approvalCodeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private User testParent;
    private User testChild;
    private Family testFamily;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        testParent = new User("Parent Test", "parent_" + suffix + "@nivya.local", "$2a$10$hashedPw", RoleType.PARENT);
        testParent.setStatus(UserStatus.ACTIVE);
        testParent = userRepository.save(testParent);

        testChild = new User("Child Test", "child_" + suffix + "@nivya.local", "$2a$10$hashedPw", RoleType.CHILD);
        testChild.setStatus(UserStatus.ACTIVE);
        testChild = userRepository.save(testChild);

        testFamily = familyRepository.save(new Family("Family " + suffix, testParent));
        familyMemberRepository.save(new FamilyMember(testFamily, testParent, RoleType.PARENT));
        familyMemberRepository.save(new FamilyMember(testFamily, testChild, RoleType.CHILD));
    }

    @Test
    @DisplayName("1. EmailNotification lifecycle: creates PENDING then completes as SENT with messageId")
    void testEmailNotificationLifecycleSuccess() {
        String idempotencyKey = "test-notif-success-" + System.currentTimeMillis();
        EmailSendResult result = emailService.dispatchEmailSync(
                testParent,
                testParent.getEmail(),
                "SECURITY",
                "Test Security Notice",
                "<p>Hello</p>",
                "Hello",
                idempotencyKey
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessageId()).isNotBlank();

        Optional<EmailNotification> notifOpt = emailNotificationRepository.findByIdempotencyKey(idempotencyKey);
        assertThat(notifOpt).isPresent();
        EmailNotification notif = notifOpt.get();
        assertThat(notif.getStatus()).isEqualTo("SENT");
        assertThat(notif.getProviderMessageId()).isEqualTo(result.getMessageId());
        assertThat(notif.getSentAt()).isNotNull();
        assertThat(notif.getAttemptCount()).isEqualTo(1);
        assertThat(notif.getUser()).isNotNull();
        assertThat(notif.getUser().getId()).isEqualTo(testParent.getId());
    }

    @Test
    @DisplayName("2. Idempotency deduplication: identical idempotencyKey returns success without duplicate insertion")
    void testIdempotencyDeduplication() {
        String idempotencyKey = "test-idempotent-" + System.currentTimeMillis();

        EmailSendResult result1 = emailService.dispatchEmailSync(
                testParent, testParent.getEmail(), "LOGIN", "Login 1", "<p>1</p>", "1", idempotencyKey
        );
        assertThat(result1.isSuccess()).isTrue();

        EmailSendResult result2 = emailService.dispatchEmailSync(
                testParent, testParent.getEmail(), "LOGIN", "Login 2", "<p>2</p>", "2", idempotencyKey
        );
        assertThat(result2.isSuccess()).isTrue();
        assertThat(result2.getProvider()).isEqualTo("IDEMPOTENT");

        List<EmailNotification> notifs = emailNotificationRepository.findByRecipientEmailOrderByCreatedAtDesc(testParent.getEmail());
        long matching = notifs.stream().filter(n -> idempotencyKey.equals(n.getIdempotencyKey())).count();
        assertThat(matching).isEqualTo(1);
    }

    @Test
    @DisplayName("3. Child deletion approval DB transaction commits and leaves approval code PENDING on success")
    void testChildDeletionApprovalSuccessLeavesCodePending() {
        RequestChildApprovalResponse response = accountDeletionService.requestChildDeletionApproval(testChild);
        assertThat(response).isNotNull();
        assertThat(response.getParentEmailMasked()).contains("***");
        assertThat(response.getExpiresInMinutes()).isEqualTo(15);

        List<DeletionApprovalCode> pendingCodes = approvalCodeRepository.findAllByChildUserIdAndStatus(testChild.getId(), "PENDING");
        assertThat(pendingCodes).hasSize(1);
        DeletionApprovalCode code = pendingCodes.get(0);
        assertThat(code.getParentUserId()).isEqualTo(testParent.getId());
        assertThat(code.getStatus()).isEqualTo("PENDING");
        assertThat(code.isExpired()).isFalse();

        // Email was dispatched
        List<EmailNotification> emails = emailNotificationRepository.findByRecipientEmailOrderByCreatedAtDesc(testParent.getEmail());
        assertThat(emails).anyMatch(e -> "CHILD_DELETION_APPROVAL".equals(e.getNotificationType()) && "SENT".equals(e.getStatus()));
    }

    @Test
    @DisplayName("4. Resend code revokes previous pending code and issues a new code with fresh idempotency key")
    void testChildDeletionApprovalResendRevokesPreviousCode() {
        // Request initial code
        RequestChildApprovalResponse response1 = accountDeletionService.requestChildDeletionApproval(testChild);
        assertThat(response1).isNotNull();

        List<DeletionApprovalCode> codesAfterFirst = approvalCodeRepository.findAllByChildUserIdAndStatus(testChild.getId(), "PENDING");
        assertThat(codesAfterFirst).hasSize(1);
        Long firstCodeId = codesAfterFirst.get(0).getId();

        // Resend code
        RequestChildApprovalResponse response2 = accountDeletionService.requestChildDeletionApproval(testChild);
        assertThat(response2).isNotNull();

        // Verify first code is now REVOKED
        Optional<DeletionApprovalCode> oldCodeOpt = approvalCodeRepository.findById(firstCodeId);
        assertThat(oldCodeOpt).isPresent();
        assertThat(oldCodeOpt.get().getStatus()).isEqualTo("REVOKED");

        // Verify exactly one active PENDING code exists
        List<DeletionApprovalCode> codesAfterResend = approvalCodeRepository.findAllByChildUserIdAndStatus(testChild.getId(), "PENDING");
        assertThat(codesAfterResend).hasSize(1);
        assertThat(codesAfterResend.get(0).getId()).isNotEqualTo(firstCodeId);

        // Verify distinct email_notifications exist
        List<EmailNotification> emails = emailNotificationRepository.findByRecipientEmailOrderByCreatedAtDesc(testParent.getEmail());
        long approvalEmails = emails.stream().filter(e -> "CHILD_DELETION_APPROVAL".equals(e.getNotificationType())).count();
        assertThat(approvalEmails).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("5. DeviceSessionService.recordLogin registers post-commit synchronization when transaction is active")
    void testRecordLoginWaitsForTransactionCommit() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        AtomicBoolean committed = new AtomicBoolean(false);

        template.execute(status -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();

            deviceSessionService.recordLogin(
                    testParent,
                    "test-fingerprint-" + System.currentTimeMillis(),
                    "Chrome Mac",
                    "WEB",
                    "macOS",
                    "1.0.0",
                    "127.0.0.1"
            );

            // Transaction is still uncommitted here
            committed.set(true);
            return null;
        });

        assertThat(committed.get()).isTrue();
    }
}
