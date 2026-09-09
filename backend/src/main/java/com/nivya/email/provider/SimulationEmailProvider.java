package com.nivya.email.provider;

import com.nivya.email.dto.EmailSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SimulationEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(SimulationEmailProvider.class);

    @Override
    public String getProviderName() {
        return "SIMULATION";
    }

    @Override
    public EmailSendResult sendEmail(String to, String subject, String bodyHtml, String bodyText) {
        String msgId = "sim-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[SIMULATION EMAIL] Delivered messageId={} to={} subject='{}'", msgId, to, subject);
        return EmailSendResult.success(getProviderName(), msgId);
    }
}
