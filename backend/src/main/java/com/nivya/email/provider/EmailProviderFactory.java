package com.nivya.email.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Factory for resolving active and requested EmailProvider instances.
 * Guarantees explicit provider resolution and prevents silent fallback
 * when SMTP is configured.
 */
@Component
public class EmailProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(EmailProviderFactory.class);

    private final Map<String, EmailProvider> providers;
    private final String activeProviderName;

    public EmailProviderFactory(
            List<EmailProvider> providerList,
            @Value("${nivya.email.provider:SIMULATION}") String activeProviderName) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(p -> p.getProviderName().toUpperCase(), p -> p));
        this.activeProviderName = activeProviderName != null ? activeProviderName.trim().toUpperCase() : "SIMULATION";

        if (!this.providers.containsKey(this.activeProviderName)) {
            log.error("Configured email provider '{}' is not registered. Available providers: {}",
                    this.activeProviderName, this.providers.keySet());
        } else {
            log.info("Initialized EmailProviderFactory with active provider: {}", this.activeProviderName);
        }
    }

    /**
     * Resolves the primary configured EmailProvider.
     * Explicitly returns the configured provider (e.g. SmtpEmailProvider when EMAIL_PROVIDER=SMTP).
     * Does NOT silently substitute SIMULATION when SMTP is selected.
     */
    public EmailProvider getProvider() {
        EmailProvider provider = providers.get(activeProviderName);
        if (provider != null) {
            return provider;
        }

        throw new IllegalArgumentException("Configured email provider '" + activeProviderName +
                "' is not registered. Registered providers: " + providers.keySet());
    }

    public EmailProvider getProvider(String name) {
        if (name == null || name.isBlank()) {
            return getProvider();
        }
        String key = name.trim().toUpperCase();
        EmailProvider provider = providers.get(key);
        if (provider != null) {
            return provider;
        }
        throw new IllegalArgumentException("Requested email provider '" + name +
                "' is not registered. Registered providers: " + providers.keySet());
    }

    public String getActiveProviderName() {
        return activeProviderName;
    }
}
