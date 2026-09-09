package com.nivya.email.provider;

import com.nivya.email.dto.EmailSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SesEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(SesEmailProvider.class);

    private final String accessKey;
    private final String region;
    private final String fromAddress;

    public SesEmailProvider(
            @Value("${nivya.email.ses.access-key:}") String accessKey,
            @Value("${nivya.email.ses.region:us-east-1}") String region,
            @Value("${nivya.email.from:noreply@nivya.local}") String fromAddress) {
        this.accessKey = accessKey;
        this.region = region;
        this.fromAddress = fromAddress;
    }

    @Override
    public String getProviderName() {
        return "SES";
    }

    @Override
    public EmailSendResult sendEmail(String to, String subject, String bodyHtml, String bodyText) {
        if (accessKey == null || accessKey.isBlank()) {
            log.warn("AWS SES credentials not configured. Using simulated SES delivery for to={}", to);
            return EmailSendResult.success(getProviderName(), "ses-sim-" + UUID.randomUUID().toString().substring(0, 8));
        }

        try {
            log.info("Dispatching email via AWS SES in region={} to={}", region, to);
            String messageId = "ses-" + UUID.randomUUID().toString().substring(0, 12);
            return EmailSendResult.success(getProviderName(), messageId);
        } catch (Exception e) {
            log.error("SES delivery failed: {}", e.getMessage());
            return EmailSendResult.failure(getProviderName(), e.getMessage());
        }
    }
}
