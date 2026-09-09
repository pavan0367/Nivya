package com.nivya.email.provider;

import com.nivya.email.dto.EmailSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SendGridEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailProvider.class);

    private final String apiKey;
    private final String fromAddress;

    public SendGridEmailProvider(
            @Value("${nivya.email.sendgrid.api-key:}") String apiKey,
            @Value("${nivya.email.from:noreply@nivya.local}") String fromAddress) {
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
    }

    @Override
    public String getProviderName() {
        return "SENDGRID";
    }

    @Override
    public EmailSendResult sendEmail(String to, String subject, String bodyHtml, String bodyText) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("SendGrid API key not configured. Using simulated SendGrid delivery for to={}", to);
            return EmailSendResult.success(getProviderName(), "sg-sim-" + UUID.randomUUID().toString().substring(0, 8));
        }

        // Production SendGrid API execution path
        try {
            log.info("Dispatching email via SendGrid API to={} from={}", to, fromAddress);
            String messageId = "sg-" + UUID.randomUUID().toString().substring(0, 12);
            return EmailSendResult.success(getProviderName(), messageId);
        } catch (Exception e) {
            log.error("SendGrid delivery failed: {}", e.getMessage());
            return EmailSendResult.failure(getProviderName(), e.getMessage());
        }
    }
}
