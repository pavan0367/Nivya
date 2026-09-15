package com.nivya.email.provider;

import com.nivya.email.dto.EmailSendResult;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SmtpEmailProviderTest {

    @Test
    @DisplayName("Provider name must be SMTP")
    void testProviderName() {
        SmtpEmailProvider provider = new SmtpEmailProvider(
                "smtp.gmail.com", 587, "", "", "noreply@nivya.com",
                true, true, true, 5000, 5000, 5000
        );
        assertThat(provider.getProviderName()).isEqualTo("SMTP");
        assertThat(provider.isConfigured()).isFalse();
    }

    @Test
    @DisplayName("Unconfigured SMTP provider returns clear failure result without throwing exception")
    void testUnconfiguredSmtpReturnsFailure() {
        SmtpEmailProvider provider = new SmtpEmailProvider(
                "smtp.gmail.com", 587, "", "", "noreply@nivya.com",
                true, true, true, 5000, 5000, 5000
        );

        EmailSendResult result = provider.sendEmail("test@example.com", "Test Subject", "<p>Html</p>", "Text");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getProvider()).isEqualTo("SMTP");
        assertThat(result.getErrorMessage()).contains("SMTP credentials unconfigured");
    }

    @Test
    @DisplayName("Configured SMTP provider sends email via JavaMailSender")
    void testConfiguredSmtpSendsEmail() {
        SmtpEmailProvider provider = new SmtpEmailProvider(
                "smtp.gmail.com", 587, "user@gmail.com", "app-password-1234", "noreply@nivya.com",
                true, true, true, 5000, 5000, 5000
        );
        assertThat(provider.isConfigured()).isTrue();

        JavaMailSenderImpl mockSender = mock(JavaMailSenderImpl.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mockSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mockSender).send(any(MimeMessage.class));

        provider.setJavaMailSender(mockSender);

        EmailSendResult result = provider.sendEmail("recipient@gmail.com", "Verification Code", "<h1>123456</h1>", "Code: 123456");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProvider()).isEqualTo("SMTP");
        assertThat(result.getMessageId()).isNotBlank();
        verify(mockSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Authentication failure produces classified error message")
    void testAuthenticationFailureHandling() {
        SmtpEmailProvider provider = new SmtpEmailProvider(
                "smtp.gmail.com", 587, "user@gmail.com", "wrong-password", "noreply@nivya.com",
                true, true, true, 5000, 5000, 5000
        );

        JavaMailSenderImpl mockSender = mock(JavaMailSenderImpl.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mockSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailAuthenticationException("535-5.7.8 Username and Password not accepted")).when(mockSender).send(any(MimeMessage.class));

        provider.setJavaMailSender(mockSender);

        EmailSendResult result = provider.sendEmail("recipient@gmail.com", "Subject", "<p>Html</p>", "Text");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("SMTP authentication failed");
    }
}
