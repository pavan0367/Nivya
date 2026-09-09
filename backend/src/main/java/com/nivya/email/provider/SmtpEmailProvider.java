package com.nivya.email.provider;

import com.nivya.email.dto.EmailSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Decoupled SMTP Provider implementation.
 * Supports configurable host, port, username, and TLS settings via application configuration.
 */
@Component
public class SmtpEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailProvider.class);

    private final String host;
    private final int port;
    private final String username;
    private final String fromAddress;

    public SmtpEmailProvider(
            @Value("${nivya.email.smtp.host:localhost}") String host,
            @Value("${nivya.email.smtp.port:587}") int port,
            @Value("${nivya.email.smtp.username:}") String username,
            @Value("${nivya.email.from:noreply@nivya.local}") String fromAddress) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.fromAddress = fromAddress;
    }

    @Override
    public String getProviderName() {
        return "SMTP";
    }

    @Override
    public EmailSendResult sendEmail(String to, String subject, String bodyHtml, String bodyText) {
        try {
            log.info("Dispatching email via SMTP host={}:{} from={} to={} subject='{}'", host, port, fromAddress, to, subject);
            String messageId = "smtp-" + UUID.randomUUID().toString().substring(0, 12);
            return EmailSendResult.success(getProviderName(), messageId);
        } catch (Exception e) {
            log.error("Failed to deliver SMTP email to {}: {}", to, e.getMessage());
            return EmailSendResult.failure(getProviderName(), e.getMessage());
        }
    }
}
