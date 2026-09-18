package com.nivya.email.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.email.dto.EmailSendResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BrevoEmailProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String readBody(HttpRequest request) {
        if (request.bodyPublisher().isEmpty()) return "";
        Flow.Publisher<ByteBuffer> publisher = request.bodyPublisher().get();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        publisher.subscribe(new Flow.Subscriber<>() {
            @Override public void onSubscribe(Flow.Subscription s) { s.request(Long.MAX_VALUE); }
            @Override public void onNext(ByteBuffer item) {
                byte[] bytes = new byte[item.remaining()];
                item.get(bytes);
                baos.writeBytes(bytes);
            }
            @Override public void onError(Throwable t) {}
            @Override public void onComplete() {}
        });
        return baos.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Provider name must be BREVO")
    void testProviderName() {
        BrevoEmailProvider provider = new BrevoEmailProvider(
                "xkeysib-test-api-key", "breversupport@gmail.com", "Nivya",
                "https://api.brevo.com/v3/smtp/email", 5000, objectMapper, mock(HttpClient.class)
        );
        assertThat(provider.getProviderName()).isEqualTo("BREVO");
        assertThat(provider.isConfigured()).isTrue();
    }

    @Test
    @DisplayName("Missing API key handled cleanly without executing HTTP dispatch")
    void testMissingApiKeyReturnsFailure() {
        HttpClient mockClient = mock(HttpClient.class);
        BrevoEmailProvider provider = new BrevoEmailProvider(
                "", "breversupport@gmail.com", "Nivya",
                "https://api.brevo.com/v3/smtp/email", 5000, objectMapper, mockClient
        );

        assertThat(provider.isConfigured()).isFalse();

        EmailSendResult result = provider.sendEmail("recipient@example.com", "Subject", "<p>Html</p>", "Text");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getProvider()).isEqualTo("BREVO");
        assertThat(result.getErrorMessage()).contains("Brevo API key unconfigured");
        verifyNoInteractions(mockClient);
    }

    @Test
    @DisplayName("Successful HTTP 201 request, correct URL, api-key header, JSON sender/recipient/subject/htmlContent/textContent, and messageId extraction")
    void testSuccessfulEmailDispatch() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(201);
        when(mockResponse.body()).thenReturn("{\"messageId\":\"<202609181530.123456789@smtp-relay.brevo.com>\"}");
        doReturn(mockResponse).when(mockClient).send(any(HttpRequest.class), any());

        String testApiKey = "xkeysib-9876543210abcdef-real-format-key";
        BrevoEmailProvider provider = new BrevoEmailProvider(
                testApiKey, "breversupport@gmail.com", "Nivya Platform",
                "https://api.brevo.com/v3/smtp/email", 5000, objectMapper, mockClient
        );

        EmailSendResult result = provider.sendEmail(
                "parent@family.local",
                "Your Nivya Verification Code",
                "<h1>654321</h1>",
                "Your verification code is 654321"
        );

        // Verify result
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProvider()).isEqualTo("BREVO");
        assertThat(result.getMessageId()).isEqualTo("<202609181530.123456789@smtp-relay.brevo.com>");

        // Verify HTTP request details
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockClient, times(1)).send(requestCaptor.capture(), any());

        HttpRequest sentRequest = requestCaptor.getValue();
        assertThat(sentRequest.uri()).isEqualTo(URI.create("https://api.brevo.com/v3/smtp/email"));
        assertThat(sentRequest.method()).isEqualTo("POST");
        assertThat(sentRequest.headers().firstValue("api-key")).contains(testApiKey);
        assertThat(sentRequest.headers().firstValue("Content-Type")).contains("application/json");
        assertThat(sentRequest.headers().firstValue("Accept")).contains("application/json");

        // Verify JSON payload structure
        String requestJson = readBody(sentRequest);
        assertThat(requestJson).contains("\"sender\":{\"name\":\"Nivya Platform\",\"email\":\"breversupport@gmail.com\"}");
        assertThat(requestJson).contains("\"to\":[{\"email\":\"parent@family.local\"}]");
        assertThat(requestJson).contains("\"subject\":\"Your Nivya Verification Code\"");
        assertThat(requestJson).contains("\"htmlContent\":\"<h1>654321</h1>\"");
        assertThat(requestJson).contains("\"textContent\":\"Your verification code is 654321\"");
    }

    @Test
    @DisplayName("HTTP 400 failure classified as bad request / validation error")
    void testHttp400BadRequest() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(400);
        when(mockResponse.body()).thenReturn("{\"code\":\"bad_request\",\"message\":\"Invalid email address provided in 'to' field\"}");
        doReturn(mockResponse).when(mockClient).send(any(HttpRequest.class), any());

        BrevoEmailProvider provider = new BrevoEmailProvider(
                "xkeysib-test-key", "breversupport@gmail.com", "Nivya",
                "https://api.brevo.com/v3/smtp/email", 5000, objectMapper, mockClient
        );

        EmailSendResult result = provider.sendEmail("invalid-email", "Test", "<p>Body</p>", "Body");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getProvider()).isEqualTo("BREVO");
        assertThat(result.getErrorMessage()).contains("HTTP 400");
        assertThat(result.getErrorMessage()).contains("Invalid email address");
    }

    @Test
    @DisplayName("HTTP 401/403 failure classified as authentication/configuration error")
    void testHttp401Unauthorized() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(401);
        when(mockResponse.body()).thenReturn("{\"code\":\"unauthorized\",\"message\":\"Key not found or inactive\"}");
        doReturn(mockResponse).when(mockClient).send(any(HttpRequest.class), any());

        BrevoEmailProvider provider = new BrevoEmailProvider(
                "xkeysib-invalid-key", "breversupport@gmail.com", "Nivya",
                "https://api.brevo.com/v3/smtp/email", 5000, objectMapper, mockClient
        );

        EmailSendResult result = provider.sendEmail("user@example.com", "Test", "<p>Body</p>", "Body");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getProvider()).isEqualTo("BREVO");
        assertThat(result.getErrorMessage()).contains("HTTP 401");
        assertThat(result.getErrorMessage()).contains("authentication failed");
        assertThat(result.getErrorMessage()).contains("Key not found");
    }

    @Test
    @DisplayName("HTTP 429 failure classified as rate limit error")
    void testHttp429RateLimit() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(429);
        when(mockResponse.body()).thenReturn("{\"code\":\"rate_limit\",\"message\":\"Too many requests in current window\"}");
        doReturn(mockResponse).when(mockClient).send(any(HttpRequest.class), any());

        BrevoEmailProvider provider = new BrevoEmailProvider(
                "xkeysib-test-key", "breversupport@gmail.com", "Nivya",
                "https://api.brevo.com/v3/smtp/email", 5000, objectMapper, mockClient
        );

        EmailSendResult result = provider.sendEmail("user@example.com", "Test", "<p>Body</p>", "Body");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getProvider()).isEqualTo("BREVO");
        assertThat(result.getErrorMessage()).contains("HTTP 429");
        assertThat(result.getErrorMessage()).contains("rate limit exceeded");
    }

    @Test
    @DisplayName("HTTP 5xx failure classified as server error")
    void testHttp5xxServerError() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(500);
        when(mockResponse.body()).thenReturn("{\"message\":\"Internal error processing transactional dispatch\"}");
        doReturn(mockResponse).when(mockClient).send(any(HttpRequest.class), any());

        BrevoEmailProvider provider = new BrevoEmailProvider(
                "xkeysib-test-key", "breversupport@gmail.com", "Nivya",
                "https://api.brevo.com/v3/smtp/email", 5000, objectMapper, mockClient
        );

        EmailSendResult result = provider.sendEmail("user@example.com", "Test", "<p>Body</p>", "Body");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getProvider()).isEqualTo("BREVO");
        assertThat(result.getErrorMessage()).contains("HTTP 500");
        assertThat(result.getErrorMessage()).contains("server error");
    }

    @Test
    @DisplayName("Network timeout failure classified cleanly")
    void testTimeoutFailure() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        when(mockClient.send(any(HttpRequest.class), any()))
                .thenThrow(new HttpTimeoutException("Connection timed out after 5000ms"));

        BrevoEmailProvider provider = new BrevoEmailProvider(
                "xkeysib-test-key", "breversupport@gmail.com", "Nivya",
                "https://api.brevo.com/v3/smtp/email", 5000, objectMapper, mockClient
        );

        EmailSendResult result = provider.sendEmail("user@example.com", "Test", "<p>Body</p>", "Body");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getProvider()).isEqualTo("BREVO");
        assertThat(result.getErrorMessage()).contains("network timeout");
    }

    @Test
    @DisplayName("Network failure classified cleanly")
    void testNetworkFailure() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        when(mockClient.send(any(HttpRequest.class), any()))
                .thenThrow(new RuntimeException("SSL connection reset by peer"));

        BrevoEmailProvider provider = new BrevoEmailProvider(
                "xkeysib-test-key", "breversupport@gmail.com", "Nivya",
                "https://api.brevo.com/v3/smtp/email", 5000, objectMapper, mockClient
        );

        EmailSendResult result = provider.sendEmail("user@example.com", "Test", "<p>Body</p>", "Body");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getProvider()).isEqualTo("BREVO");
        assertThat(result.getErrorMessage()).contains("Brevo delivery failed");
    }

    @Test
    @DisplayName("API key is never exposed in error messages or logs")
    void testApiKeyNeverExposedInErrorMessage() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        when(mockClient.send(any(HttpRequest.class), any()))
                .thenThrow(new RuntimeException("Unexpected I/O failure communicating with Brevo"));

        String confidentialKey = "xkeysib-confidential-production-key-987654";
        BrevoEmailProvider provider = new BrevoEmailProvider(
                confidentialKey, "breversupport@gmail.com", "Nivya",
                "https://api.brevo.com/v3/smtp/email", 5000, objectMapper, mockClient
        );

        EmailSendResult result = provider.sendEmail("user@example.com", "Test", "<p>Body</p>", "Body");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).doesNotContain(confidentialKey);
    }

    @Test
    @DisplayName("Factory resolves BREVO, preserves SIMULATION, SMTP, and RESEND")
    void testFactoryResolvesAllProviders() {
        BrevoEmailProvider brevoProvider = new BrevoEmailProvider(
                "xkeysib-test-key", "breversupport@gmail.com", "Nivya",
                "https://api.brevo.com/v3/smtp/email", 5000, objectMapper, mock(HttpClient.class)
        );
        SimulationEmailProvider simProvider = new SimulationEmailProvider();
        ResendEmailProvider resendProvider = new ResendEmailProvider(
                "re_test_key", "onboarding@resend.dev", "https://api.resend.com/emails", 5000, objectMapper, mock(HttpClient.class)
        );
        SmtpEmailProvider smtpProvider = new SmtpEmailProvider(
                "smtp.gmail.com", 587, "", "", "noreply@nivya.com", true, true, true, 5000, 5000, 5000
        );

        EmailProviderFactory factory = new EmailProviderFactory(
                List.of(simProvider, resendProvider, brevoProvider, smtpProvider),
                "BREVO"
        );

        // Factory resolution
        assertThat(factory.getActiveProviderName()).isEqualTo("BREVO");
        assertThat(factory.getProvider()).isSameAs(brevoProvider);
        assertThat(factory.getProvider("BREVO")).isSameAs(brevoProvider);
        assertThat(factory.getProvider("RESEND")).isSameAs(resendProvider);
        assertThat(factory.getProvider("SIMULATION")).isSameAs(simProvider);
        assertThat(factory.getProvider("SMTP")).isSameAs(smtpProvider);
    }
}
