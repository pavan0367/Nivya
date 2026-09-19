package com.nivya.email;

import com.nivya.audit.service.AuditService;
import com.nivya.email.dto.EmailSendResult;
import com.nivya.email.entity.EmailNotification;
import com.nivya.email.provider.EmailProvider;
import com.nivya.email.provider.EmailProviderFactory;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailSubjectThreadingTest {

    @Mock
    private EmailProviderFactory providerFactory;

    @Mock
    private EmailProvider mockProvider;

    @Mock
    private EmailNotificationRepository notificationRepository;

    @Mock
    private EmailPreferenceRepository preferenceRepository;

    @Mock
    private EmailVerificationCodeRepository verificationCodeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

    private EmailService emailService;
    private User testUser;
    private AtomicLong idSequence;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);

        emailService = new EmailService(
                providerFactory,
                notificationRepository,
                preferenceRepository,
                verificationCodeRepository,
                userRepository,
                auditService,
                transactionManager
        );

        when(providerFactory.getProvider()).thenReturn(mockProvider);
        when(mockProvider.getProviderName()).thenReturn("MOCK_PROVIDER");

        idSequence = new AtomicLong(48217);
        when(notificationRepository.saveAndFlush(any(EmailNotification.class)))
                .thenAnswer(invocation -> {
                    EmailNotification n = invocation.getArgument(0);
                    if (n.getId() == null) {
                        n.setId(idSequence.getAndIncrement());
                    }
                    return n;
                });

        testUser = new User("Parent User", "parent@nivya.local", "$2a$10$pw", RoleType.PARENT);
        testUser.setId(101L);
        testUser.setStatus(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("1. Two separate email sends of the same notification type receive different subjects with distinct #id")
    void testTwoSeparateEmailsReceiveDifferentSubjects() {
        when(mockProvider.sendEmail(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(EmailSendResult.success("MOCK_PROVIDER", "msg-1"))
                .thenReturn(EmailSendResult.success("MOCK_PROVIDER", "msg-2"));

        String baseSubject = "Nivya Security Alert: New Login to Your Account";
        String recipient = testUser.getEmail();

        EmailSendResult res1 = emailService.dispatchEmailSync(
                testUser, recipient, "LOGIN", baseSubject, "<p>Login 1</p>", "Login 1", "key-1"
        );
        assertThat(res1.isSuccess()).isTrue();

        EmailSendResult res2 = emailService.dispatchEmailSync(
                testUser, recipient, "LOGIN", baseSubject, "<p>Login 2</p>", "Login 2", "key-2"
        );
        assertThat(res2.isSuccess()).isTrue();

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockProvider, times(2)).sendEmail(eq(recipient), subjectCaptor.capture(), anyString(), anyString());

        List<String> capturedSubjects = subjectCaptor.getAllValues();
        String subject1 = capturedSubjects.get(0);
        String subject2 = capturedSubjects.get(1);

        assertThat(subject1).isEqualTo(baseSubject + " #48217");
        assertThat(subject2).isEqualTo(baseSubject + " #48218");
        assertThat(subject1).isNotEqualTo(subject2);
    }

    @Test
    @DisplayName("2. Retries of the same EmailNotification preserve the exact same subject and reference")
    void testRetryPreservesSameSubject() {
        AtomicInteger attemptCounter = new AtomicInteger(0);
        when(mockProvider.sendEmail(anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    int attempt = attemptCounter.incrementAndGet();
                    if (attempt == 1) {
                        return EmailSendResult.failure("MOCK_PROVIDER", "Transient network timeout");
                    }
                    return EmailSendResult.success("MOCK_PROVIDER", "msg-success-retry");
                });

        String baseSubject = "Nivya - Security Alert";
        String recipient = testUser.getEmail();

        EmailSendResult result = emailService.dispatchEmailSync(
                testUser, recipient, "SECURITY", baseSubject, "<p>Body</p>", "Body", "retry-key"
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(attemptCounter.get()).isEqualTo(2);

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockProvider, times(2)).sendEmail(eq(recipient), subjectCaptor.capture(), anyString(), anyString());

        List<String> subjects = subjectCaptor.getAllValues();
        assertThat(subjects).hasSize(2);
        // Both attempts of the same EmailNotification must use the exact same subject
        assertThat(subjects.get(0)).isEqualTo(baseSubject + " #48217");
        assertThat(subjects.get(1)).isEqualTo(baseSubject + " #48217");
        assertThat(subjects.get(0)).isEqualTo(subjects.get(1));
    }

    @Test
    @DisplayName("3. Child approval resend receives a distinct subject from previous request")
    void testChildApprovalResendReceivesDifferentSubject() {
        when(mockProvider.sendEmail(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(EmailSendResult.success("MOCK_PROVIDER", "msg-initial"))
                .thenReturn(EmailSendResult.success("MOCK_PROVIDER", "msg-resend"));

        String baseSubject = "Nivya - Child Account Deletion Request";
        String recipient = testUser.getEmail();

        // 1. Initial approval request dispatch
        EmailSendResult initialResult = emailService.dispatchEmailSync(
                testUser, recipient, "CHILD_DELETION_APPROVAL", baseSubject,
                "<p>Initial Approval Code: 123456</p>", "Initial Approval Code: 123456", "approval-initial"
        );
        assertThat(initialResult.isSuccess()).isTrue();

        // 2. Resend code request dispatch
        EmailSendResult resendResult = emailService.dispatchEmailSync(
                testUser, recipient, "CHILD_DELETION_APPROVAL", baseSubject,
                "<p>Resent Approval Code: 654321</p>", "Resent Approval Code: 654321", "approval-resend"
        );
        assertThat(resendResult.isSuccess()).isTrue();

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockProvider, times(2)).sendEmail(eq(recipient), subjectCaptor.capture(), anyString(), anyString());

        List<String> subjects = subjectCaptor.getAllValues();
        String initialSubject = subjects.get(0);
        String resendSubject = subjects.get(1);

        assertThat(initialSubject).isEqualTo("Nivya - Child Account Deletion Request #48217");
        assertThat(resendSubject).isEqualTo("Nivya - Child Account Deletion Request #48218");
        assertThat(initialSubject).isNotEqualTo(resendSubject);
    }

    @Test
    @DisplayName("4. Existing email bodies and notification types remain intact and unchanged")
    void testExistingEmailBodiesAndNotificationTypesRemainIntact() {
        when(mockProvider.sendEmail(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(EmailSendResult.success("MOCK_PROVIDER", "msg-body-check"));

        String htmlBody = "<html><body><p>Parent action required: Delete Account</p></body></html>";
        String textBody = "Parent action required: Delete Account";

        emailService.dispatchEmailSync(
                testUser, testUser.getEmail(), "CHILD_DELETION_APPROVAL",
                "Nivya - Child Account Deletion Request", htmlBody, textBody, "key-body"
        );

        ArgumentCaptor<String> bodyHtmlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyTextCaptor = ArgumentCaptor.forClass(String.class);

        verify(mockProvider).sendEmail(
                eq(testUser.getEmail()), anyString(), bodyHtmlCaptor.capture(), bodyTextCaptor.capture()
        );

        assertThat(bodyHtmlCaptor.getValue()).isEqualTo(htmlBody);
        assertThat(bodyTextCaptor.getValue()).isEqualTo(textBody);

        ArgumentCaptor<EmailNotification> notifCaptor = ArgumentCaptor.forClass(EmailNotification.class);
        verify(notificationRepository, atLeastOnce()).saveAndFlush(notifCaptor.capture());
        assertThat(notifCaptor.getValue().getNotificationType()).isEqualTo("CHILD_DELETION_APPROVAL");
    }

    @Test
    @DisplayName("5. Subject format uses numeric #id, never exposes OTP codes or long random UUIDs")
    void testSubjectDoesNotExposeOtpOrUuids() {
        when(mockProvider.sendEmail(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(EmailSendResult.success("MOCK_PROVIDER", "msg-fmt"));

        String baseSubject = "Nivya Security Alert: New Login to Your Account";
        String approvalCode = "789123";

        emailService.dispatchEmailSync(
                testUser, testUser.getEmail(), "LOGIN", baseSubject,
                "<p>Code is " + approvalCode + "</p>", "Code is " + approvalCode, "key-fmt"
        );

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockProvider).sendEmail(eq(testUser.getEmail()), subjectCaptor.capture(), anyString(), anyString());

        String subject = subjectCaptor.getValue();
        // Pattern matches: "<Base Subject> #<digits>"
        Pattern expectedPattern = Pattern.compile("^" + Pattern.quote(baseSubject) + " #\\d+$");
        assertThat(expectedPattern.matcher(subject).matches())
                .withFailMessage("Subject '%s' does not match expected format '<Base Subject> #<digits>'", subject)
                .isTrue();

        // Must not expose OTP / 6-digit code in subject
        assertThat(subject).doesNotContain(approvalCode);

        // Must not contain 36-char UUID pattern
        Pattern uuidPattern = Pattern.compile("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}", Pattern.CASE_INSENSITIVE);
        assertThat(uuidPattern.matcher(subject).find())
                .withFailMessage("Subject contains UUID: %s", subject)
                .isFalse();
    }
}
