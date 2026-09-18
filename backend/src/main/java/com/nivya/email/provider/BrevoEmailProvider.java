package com.nivya.email.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.email.dto.EmailSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Production HTTPS Transactional Email Provider using Brevo REST API (https://api.brevo.com/v3/smtp/email).
 * Communicates via HTTPS on port 443 with api-key header authentication, fully compatible with Render Free
 * and restricted container environments where SMTP ports 25, 465, and 587 are blocked.
 */
@Component
public class BrevoEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailProvider.class);

    private final String apiKey;
    private final String fromEmail;
    private final String fromName;
    private final String apiUrl;
    private final int timeoutMs;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public BrevoEmailProvider(
            @Value("${nivya.email.brevo.api-key:}") String apiKey,
            @Value("${nivya.email.brevo.from:${nivya.email.from:breversupport@gmail.com}}") String fromEmail,
            @Value("${nivya.email.brevo.from-name:Nivya}") String fromName,
            @Value("${nivya.email.brevo.api-url:https://api.brevo.com/v3/smtp/email}") String apiUrl,
            @Value("${nivya.email.brevo.timeout-ms:5000}") int timeoutMs,
            ObjectMapper objectMapper) {
        this(apiKey, fromEmail, fromName, apiUrl, timeoutMs, objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(timeoutMs > 0 ? timeoutMs : 5000))
                        .build());
    }

    // Package-private constructor for testing with mock HttpClient and custom settings
    BrevoEmailProvider(
            String apiKey,
            String fromEmail,
            String fromName,
            String apiUrl,
            int timeoutMs,
            ObjectMapper objectMapper,
            HttpClient httpClient) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.fromEmail = fromEmail != null && !fromEmail.isBlank() ? fromEmail.trim() : "breversupport@gmail.com";
        this.fromName = fromName != null && !fromName.isBlank() ? fromName.trim() : "Nivya";
        this.apiUrl = apiUrl != null && !apiUrl.isBlank() ? apiUrl.trim() : "https://api.brevo.com/v3/smtp/email";
        this.timeoutMs = timeoutMs > 0 ? timeoutMs : 5000;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.httpClient = httpClient != null ? httpClient : HttpClient.newHttpClient();

        log.info("Initialized BrevoEmailProvider: from={} ({}), apiUrl={}, configured={}",
                this.fromEmail, this.fromName, this.apiUrl, isConfigured());
    }

    public boolean isConfigured() {
        return !this.apiKey.isBlank();
    }

    @Override
    public String getProviderName() {
        return "BREVO";
    }

    @Override
    public EmailSendResult sendEmail(String to, String subject, String bodyHtml, String bodyText) {
        if (!isConfigured()) {
            String guidance = "Brevo API key unconfigured. Set BREVO_API_KEY in environment variables (or set EMAIL_PROVIDER to SIMULATION).";
            log.warn("Cannot dispatch email via Brevo to {}: {}", to, guidance);
            return EmailSendResult.failure(getProviderName(), guidance);
        }

        if (to == null || to.isBlank()) {
            return EmailSendResult.failure(getProviderName(), "Destination email recipient cannot be empty");
        }

        try {
            log.info("Dispatching email via Brevo HTTPS API to={} from={} ({}) subject='{}'",
                    to, fromEmail, fromName, subject);

            Map<String, Object> payload = new LinkedHashMap<>();

            Map<String, String> sender = new LinkedHashMap<>();
            sender.put("name", fromName);
            sender.put("email", fromEmail);
            payload.put("sender", sender);

            Map<String, String> recipient = new LinkedHashMap<>();
            recipient.put("email", to.trim());
            payload.put("to", List.of(recipient));

            payload.put("subject", subject != null ? subject : "");
            if (bodyHtml != null && !bodyHtml.isBlank()) {
                payload.put("htmlContent", bodyHtml);
            }
            if (bodyText != null && !bodyText.isBlank()) {
                payload.put("textContent", bodyText);
            }

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int statusCode = response.statusCode();

            if (statusCode >= 200 && statusCode < 300) {
                String messageId = parseMessageId(response.body());
                log.info("Successfully delivered Brevo email messageId={} to={}", messageId, to);
                return EmailSendResult.success(getProviderName(), messageId);
            } else {
                String errorMessage = parseErrorMessage(statusCode, response.body());
                log.error("Brevo API rejected email delivery to {} with HTTP {}: {}", to, statusCode, errorMessage);
                return EmailSendResult.failure(getProviderName(), errorMessage);
            }

        } catch (HttpTimeoutException e) {
            String timeoutMsg = "Brevo API network timeout after " + timeoutMs + "ms: " + e.getMessage();
            log.error("Failed to deliver Brevo email to {}: {}", to, timeoutMsg);
            return EmailSendResult.failure(getProviderName(), timeoutMsg);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String interruptedMsg = "Brevo API dispatch interrupted: " + e.getMessage();
            log.error("Failed to deliver Brevo email to {}: {}", to, interruptedMsg);
            return EmailSendResult.failure(getProviderName(), interruptedMsg);
        } catch (Exception e) {
            String genericError = "Brevo delivery failed: " + e.getMessage();
            log.error("Failed to dispatch email via Brevo to {}: {}", to, genericError);
            return EmailSendResult.failure(getProviderName(), genericError);
        }
    }

    private String parseMessageId(String responseBody) {
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(responseBody);
                if (root.hasNonNull("messageId")) {
                    return root.get("messageId").asText();
                } else if (root.hasNonNull("id")) {
                    return root.get("id").asText();
                }
            } catch (Exception e) {
                log.warn("Could not parse 'messageId' from Brevo success response: {}", e.getMessage());
            }
        }
        return "brevo-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private String parseErrorMessage(int statusCode, String responseBody) {
        String classification;
        if (statusCode == 400) {
            classification = "Brevo validation/bad request error (HTTP 400)";
        } else if (statusCode == 401 || statusCode == 403) {
            classification = "Brevo authentication failed (HTTP " + statusCode + ") - check BREVO_API_KEY";
        } else if (statusCode == 429) {
            classification = "Brevo rate limit exceeded (HTTP 429)";
        } else if (statusCode >= 500) {
            classification = "Brevo server error (HTTP " + statusCode + ")";
        } else {
            classification = "Brevo API error (HTTP " + statusCode + ")";
        }

        if (responseBody == null || responseBody.isBlank()) {
            return classification;
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.hasNonNull("message")) {
                return classification + ": " + root.get("message").asText();
            } else if (root.hasNonNull("code")) {
                return classification + ": " + root.get("code").asText();
            }
        } catch (Exception ignored) {
        }
        return classification + ": " + responseBody;
    }

    // Getters for testing
    String getApiKey() { return apiKey; }
    String getFromEmail() { return fromEmail; }
    String getFromName() { return fromName; }
    String getApiUrl() { return apiUrl; }
    int getTimeoutMs() { return timeoutMs; }
}
