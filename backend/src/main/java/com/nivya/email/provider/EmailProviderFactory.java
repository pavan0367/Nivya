package com.nivya.email.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        this.activeProviderName = activeProviderName.toUpperCase();
        log.info("Initialized EmailProviderFactory with active provider: {}", this.activeProviderName);
    }

    public EmailProvider getProvider() {
        return providers.getOrDefault(activeProviderName, providers.get("SIMULATION"));
    }

    public EmailProvider getProvider(String name) {
        if (name == null) return getProvider();
        return providers.getOrDefault(name.toUpperCase(), getProvider());
    }
}
