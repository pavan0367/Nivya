package com.nivya.email.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.email.dto.EmailSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
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
 * Production HTTPS Email Provider utilizing Resend REST API (https://api.resend.com/emails).
 * Connects over standard HTTPS (port 443) which is fully supported on Render Free and cloud containers
 * where outbound SMTP ports (25, 465, 587) are blocked.
 */
@Component
public class ResendEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailProvider.class);

    private final String apiKey;
    private final String fromEmail;
    private final String apiUrl;
    private final int timeoutMs;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @org.springframework.beans.factory.annotation.Autowired
    public ResendEmailProvider(
            @Value("${nivya.email.resend.api-key:}") String apiKey,
            @Value("${nivya.email.resend.from:${nivya.email.from:onboarding@resend.dev}}") String fromEmail,
            @Value("${nivya.email.resend.api-url:https://api.resend.com/emails}") String apiUrl,
            @Value("${nivya.email.resend.timeout-ms:5000}") int timeoutMs,
            ObjectMapper objectMapper) {
        this(apiKey, fromEmail, apiUrl, timeoutMs, objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(timeoutMs > 0 ? timeoutMs : 5000))
                        .build());
    }

    // Package-private constructor for testing with mock HttpClient and custom settings
    ResendEmailProvider(
            String apiKey,
            String fromEmail,
            String apiUrl,
            int timeoutMs,
            ObjectMapper objectMapper,
            HttpClient httpClient) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.fromEmail = fromEmail != null && !fromEmail.isBlank() ? fromEmail.trim() : "onboarding@resend.dev";
        this.apiUrl = apiUrl != null && !apiUrl.isBlank() ? apiUrl.trim() : "https://api.resend.com/emails";
        this.timeoutMs = timeoutMs > 0 ? timeoutMs : 5000;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.httpClient = httpClient != null ? httpClient : HttpClient.newHttpClient();

        log.info("Initialized ResendEmailProvider: from={}, apiUrl={}, configured={}",
                this.fromEmail, this.apiUrl, isConfigured());
    }

    public boolean isConfigured() {
        return !this.apiKey.isBlank();
    }

    @Override
    public String getProviderName() {
        return "RESEND";
    }

    @Override
    public EmailSendResult sendEmail(String to, String subject, String bodyHtml, String bodyText) {
        if (!isConfigured()) {
            String guidance = "Resend API key unconfigured. Set RESEND_API_KEY in environment variables (or set EMAIL_PROVIDER to SIMULATION).";
            log.warn("Cannot dispatch email via Resend to {}: {}", to, guidance);
            return EmailSendResult.failure(getProviderName(), guidance);
        }

        if (to == null || to.isBlank()) {
            return EmailSendResult.failure(getProviderName(), "Destination email recipient cannot be empty");
        }

        try {
            log.info("Dispatching email via Resend HTTPS API to={} from={} subject='{}'", to, fromEmail, subject);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("from", fromEmail);
            payload.put("to", List.of(to.trim()));
            payload.put("subject", subject != null ? subject : "");
            if (bodyHtml != null && !bodyHtml.isBlank()) {
                payload.put("html", bodyHtml);
            }
            if (bodyText != null && !bodyText.isBlank()) {
                payload.put("text", bodyText);
            }

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int statusCode = response.statusCode();

            if (statusCode >= 200 && statusCode < 300) {
                String messageId = parseMessageId(response.body());
                log.info("Successfully delivered Resend email messageId={} to={}", messageId, to);
                return EmailSendResult.success(getProviderName(), messageId);
            } else {
                String errorMessage = parseErrorMessage(statusCode, response.body());
                log.error("Resend API rejected email delivery to {} with HTTP {}: {}", to, statusCode, errorMessage);
                return EmailSendResult.failure(getProviderName(), errorMessage);
            }

        } catch (HttpTimeoutException e) {
            String timeoutMsg = "Resend API network timeout after " + timeoutMs + "ms: " + e.getMessage();
            log.error("Failed to deliver Resend email to {}: {}", to, timeoutMsg);
            return EmailSendResult.failure(getProviderName(), timeoutMsg);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String interruptedMsg = "Resend API dispatch interrupted: " + e.getMessage();
            log.error("Failed to deliver Resend email to {}: {}", to, interruptedMsg);
            return EmailSendResult.failure(getProviderName(), interruptedMsg);
        } catch (Exception e) {
            String genericError = "Resend delivery failed: " + e.getMessage();
            log.error("Failed to dispatch email via Resend to {}: {}", to, genericError);
            return EmailSendResult.failure(getProviderName(), genericError);
        }
    }

    private String parseMessageId(String responseBody) {
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(responseBody);
                if (root.hasNonNull("id")) {
                    return root.get("id").asText();
                }
            } catch (Exception e) {
                log.warn("Could not parse 'id' from Resend success response: {}", e.getMessage());
            }
        }
        return "resend-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private String parseErrorMessage(int statusCode, String responseBody) {
        String defaultMsg = "HTTP " + statusCode + " error from Resend API";
        if (responseBody == null || responseBody.isBlank()) {
            return defaultMsg;
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.hasNonNull("message")) {
                return defaultMsg + ": " + root.get("message").asText();
            } else if (root.hasNonNull("error")) {
                return defaultMsg + ": " + root.get("error").asText();
            }
        } catch (Exception ignored) {
        }
        return defaultMsg + ": " + responseBody;
    }

    // Getters for testing
    String getApiKey() { return apiKey; }
    String getFromEmail() { return fromEmail; }
    String getApiUrl() { return apiUrl; }
    int getTimeoutMs() { return timeoutMs; }
}
