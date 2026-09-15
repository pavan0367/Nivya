package com.nivya.email.provider;

import com.nivya.email.dto.EmailSendResult;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;

/**
 * Production SMTP Email Provider implementation.
 * Supports configurable host, port, credentials, STARTTLS, timeouts, and MIME multipart messaging.
 * Designed for standard SMTP relays, including Gmail SMTP on port 587 with App Passwords.
 */
@Component
public class SmtpEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailProvider.class);

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String fromAddress;
    private final boolean auth;
    private final boolean starttlsEnable;
    private final boolean starttlsRequired;
    private final int connectionTimeout;
    private final int timeout;
    private final int writeTimeout;

    private JavaMailSenderImpl mailSender;

    public SmtpEmailProvider(
            @Value("${nivya.email.smtp.host:smtp.gmail.com}") String host,
            @Value("${nivya.email.smtp.port:587}") int port,
            @Value("${nivya.email.smtp.username:}") String username,
            @Value("${nivya.email.smtp.password:}") String password,
            @Value("${nivya.email.from:noreply@nivya.com}") String fromAddress,
            @Value("${nivya.email.smtp.auth:true}") boolean auth,
            @Value("${nivya.email.smtp.starttls.enable:true}") boolean starttlsEnable,
            @Value("${nivya.email.smtp.starttls.required:true}") boolean starttlsRequired,
            @Value("${nivya.email.smtp.connection-timeout:5000}") int connectionTimeout,
            @Value("${nivya.email.smtp.timeout:5000}") int timeout,
            @Value("${nivya.email.smtp.write-timeout:5000}") int writeTimeout) {
        this.host = host != null ? host.trim() : "smtp.gmail.com";
        this.port = port > 0 ? port : 587;
        this.username = username != null ? username.trim() : "";
        this.password = password != null ? password.trim() : "";
        this.fromAddress = fromAddress != null && !fromAddress.isBlank() ? fromAddress.trim() : "noreply@nivya.com";
        this.auth = auth;
        this.starttlsEnable = starttlsEnable;
        this.starttlsRequired = starttlsRequired;
        this.connectionTimeout = connectionTimeout > 0 ? connectionTimeout : 5000;
        this.timeout = timeout > 0 ? timeout : 5000;
        this.writeTimeout = writeTimeout > 0 ? writeTimeout : 5000;

        initializeMailSender();
    }

    private void initializeMailSender() {
        this.mailSender = new JavaMailSenderImpl();
        this.mailSender.setHost(this.host);
        this.mailSender.setPort(this.port);
        this.mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        if (!this.username.isBlank()) {
            this.mailSender.setUsername(this.username);
        }
        if (!this.password.isBlank()) {
            this.mailSender.setPassword(this.password);
        }

        Properties props = this.mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(this.auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(this.starttlsEnable));
        props.put("mail.smtp.starttls.required", String.valueOf(this.starttlsRequired));
        props.put("mail.smtp.ssl.trust", this.host);
        props.put("mail.smtp.connectiontimeout", String.valueOf(this.connectionTimeout));
        props.put("mail.smtp.timeout", String.valueOf(this.timeout));
        props.put("mail.smtp.writetimeout", String.valueOf(this.writeTimeout));

        log.info("Initialized SmtpEmailProvider: host={}:{}, from={}, auth={}, starttls={}, username={}, configured={}",
                this.host, this.port, this.fromAddress, this.auth, this.starttlsEnable,
                maskUsername(this.username), isConfigured());
    }

    public boolean isConfigured() {
        return !this.host.isBlank() && !this.username.isBlank() && !this.password.isBlank();
    }

    @Override
    public String getProviderName() {
        return "SMTP";
    }

    @Override
    public EmailSendResult sendEmail(String to, String subject, String bodyHtml, String bodyText) {
        if (!isConfigured()) {
            String guidance = "SMTP credentials unconfigured. Set SMTP_USERNAME and SMTP_PASSWORD in .env (or switch EMAIL_PROVIDER to SIMULATION).";
            log.warn("Cannot dispatch SMTP email to {}: {}", to, guidance);
            return EmailSendResult.failure(getProviderName(), guidance);
        }

        try {
            log.info("Dispatching email via SMTP host={}:{} from={} to={} subject='{}'",
                    host, port, fromAddress, to, subject);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            try {
                helper.setFrom(fromAddress, "Nivya Platform");
            } catch (Exception e) {
                helper.setFrom(fromAddress);
            }

            helper.setTo(to.trim());
            helper.setSubject(subject);

            String textPart = bodyText != null ? bodyText : "";
            String htmlPart = bodyHtml != null ? bodyHtml : ("<html><body><p>" + textPart.replace("\n", "<br/>") + "</p></body></html>");
            helper.setText(textPart, htmlPart);

            mailSender.send(mimeMessage);

            String messageId = mimeMessage.getMessageID();
            if (messageId == null || messageId.isBlank()) {
                messageId = "smtp-" + UUID.randomUUID().toString().substring(0, 12);
            }

            log.info("Successfully delivered SMTP email messageId={} to={}", messageId, to);
            return EmailSendResult.success(getProviderName(), messageId);

        } catch (MailAuthenticationException e) {
            String errorMsg = "SMTP authentication failed — check SMTP_USERNAME and SMTP_PASSWORD (ensure a 16-char Gmail App Password is used without spaces)";
            log.error("Failed to authenticate with SMTP server host={}:{} user={}: {}", host, port, maskUsername(username), errorMsg);
            return EmailSendResult.failure(getProviderName(), errorMsg);
        } catch (MailException | MessagingException e) {
            String errorMsg = "SMTP transmission failed: " + e.getMessage();
            log.error("Failed to deliver SMTP email to {}: {}", to, errorMsg);
            return EmailSendResult.failure(getProviderName(), errorMsg);
        } catch (Exception e) {
            String errorMsg = "Unexpected error during SMTP dispatch: " + e.getMessage();
            log.error("Failed to send SMTP email to {}: {}", to, errorMsg, e);
            return EmailSendResult.failure(getProviderName(), errorMsg);
        }
    }

    private String maskUsername(String user) {
        if (user == null || user.isBlank()) {
            return "[EMPTY]";
        }
        int atIdx = user.indexOf('@');
        if (atIdx > 2) {
            return user.substring(0, 2) + "***" + user.substring(atIdx);
        } else if (user.length() > 3) {
            return user.substring(0, 2) + "***";
        }
        return "***";
    }

    // Visible for testing
    void setJavaMailSender(JavaMailSenderImpl mailSender) {
        this.mailSender = mailSender;
    }
}
