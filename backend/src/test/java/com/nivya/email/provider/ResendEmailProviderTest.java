package com.nivya.email.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivya.email.dto.EmailSendResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ResendEmailProviderTest {

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
    @DisplayName("Provider name must be RESEND")
    void testProviderName() {
        ResendEmailProvider provider = new ResendEmailProvider(
                "re_test_key_123", "Nivya <onboarding@resend.dev>",
                "https://api.resend.com/emails", 5000, objectMapper, mock(HttpClient.class)
        );
        assertThat(provider.getProviderName()).isEqualTo("RESEND");
        assertThat(provider.isConfigured()).isTrue();
    }

    @Test
    @DisplayName("Unconfigured Resend provider returns clear failure result without calling HTTP")
    void testUnconfiguredReturnsFailure() {
        HttpClient mockClient = mock(HttpClient.class);
        ResendEmailProvider provider = new ResendEmailProvider(
                "", "onboarding@resend.dev",
                "https://api.resend.com/emails", 5000, objectMapper, mockClient
        );

        assertThat(provider.isConfigured()).isFalse();

        EmailSendResult result = provider.sendEmail("user@example.com", "Test", "<p>Hi</p>", "Hi");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getProvider()).isEqualTo("RESEND");
        assertThat(result.getErrorMessage()).contains("Resend API key unconfigured");
        verifyNoInteractions(mockClient);
    }

    @Test
    @DisplayName("1, 2, 3, 4. Successful HTTPS request construction, Bearer auth, correct payload, and messageId extraction")
    void testSuccessfulEmailDispatch() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"id\":\"re_msg_49a3999c0ce14ea6\"}");
        doReturn(mockResponse).when(mockClient).send(any(HttpRequest.class), any());

        String testApiKey = "re_test_secret_key_abcdef123456";
        ResendEmailProvider provider = new ResendEmailProvider(
                testApiKey, "Nivya Safety <onboarding@resend.dev>",
                "https://api.resend.com/emails", 5000, objectMapper, mockClient
        );

        EmailSendResult result = provider.sendEmail(
                "family.member@example.com",
                "Your Nivya Verification Code",
                "<h1>123456</h1>",
                "Your code is 123456"
        );

        // Verify result
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProvider()).isEqualTo("RESEND");
        assertThat(result.getMessageId()).isEqualTo("re_msg_49a3999c0ce14ea6");

        // Verify HTTP request construction
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockClient, times(1)).send(requestCaptor.capture(), any());

        HttpRequest sentRequest = requestCaptor.getValue();
        assertThat(sentRequest.uri()).isEqualTo(URI.create("https://api.resend.com/emails"));
        assertThat(sentRequest.method()).isEqualTo("POST");
        assertThat(sentRequest.headers().firstValue("Authorization")).contains("Bearer " + testApiKey);
        assertThat(sentRequest.headers().firstValue("Content-Type")).contains("application/json");
        assertThat(sentRequest.headers().firstValue("Accept")).contains("application/json");

        // Verify JSON payload content
        String requestJson = readBody(sentRequest);
        assertThat(requestJson).contains("\"from\":\"Nivya Safety <onboarding@resend.dev>\"");
        assertThat(requestJson).contains("\"to\":[\"family.member@example.com\"]");
        assertThat(requestJson).contains("\"subject\":\"Your Nivya Verification Code\"");
        assertThat(requestJson).contains("\"html\":\"<h1>123456</h1>\"");
        assertThat(requestJson).contains("\"text\":\"Your code is 123456\"");
    }

    @Test
    @DisplayName("5. HTTP 4xx returns classified failure result")
    void testHttp4xxFailure() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(422);
        when(mockResponse.body()).thenReturn("{\"statusCode\":422,\"name\":\"validation_error\",\"message\":\"Domain not verified\"}");
        doReturn(mockResponse).when(mockClient).send(any(HttpRequest.class), any());

        ResendEmailProvider provider = new ResendEmailProvider(
                "re_valid_format_key", "unverified@customdomain.com",
                "https://api.resend.com/emails", 5000, objectMapper, mockClient
        );

        EmailSendResult result = provider.sendEmail("user@example.com", "Test", "<p>Hi</p>", "Hi");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getProvider()).isEqualTo("RESEND");
        assertThat(result.getErrorMessage()).contains("HTTP 422");
        assertThat(result.getErrorMessage()).contains("Domain not verified");
    }

    @Test
    @DisplayName("6. HTTP 5xx returns failure result")
    void testHttp5xxFailure() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(500);
        when(mockResponse.body()).thenReturn("{\"message\":\"Internal server error on Resend platform\"}");
        doReturn(mockResponse).when(mockClient).send(any(HttpRequest.class), any());

        ResendEmailProvider provider = new ResendEmailProvider(
                "re_valid_key", "onboarding@resend.dev",
                "https://api.resend.com/emails", 5000, objectMapper, mockClient
        );

        EmailSendResult result = provider.sendEmail("user@example.com", "Test", "<p>Hi</p>", "Hi");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getProvider()).isEqualTo("RESEND");
        assertThat(result.getErrorMessage()).contains("HTTP 500");
        assertThat(result.getErrorMessage()).contains("Internal server error");
    }

    @Test
    @DisplayName("7. Network timeout returns failure result")
    void testNetworkTimeout() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        when(mockClient.send(any(HttpRequest.class), any()))
                .thenThrow(new HttpTimeoutException("Connection timed out after 5000ms"));

        ResendEmailProvider provider = new ResendEmailProvider(
                "re_valid_key", "onboarding@resend.dev",
                "https://api.resend.com/emails", 5000, objectMapper, mockClient
        );

        EmailSendResult result = provider.sendEmail("user@example.com", "Test", "<p>Hi</p>", "Hi");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getProvider()).isEqualTo("RESEND");
        assertThat(result.getErrorMessage()).contains("network timeout");
    }

    @Test
    @DisplayName("8. API key is never written to error messages or exposed")
    void testApiKeyNeverExposedInErrorMessage() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        when(mockClient.send(any(HttpRequest.class), any()))
                .thenThrow(new RuntimeException("SSL handshake failed with upstream"));

        String secretApiKey = "re_super_secret_production_resend_token_999";
        ResendEmailProvider provider = new ResendEmailProvider(
                secretApiKey, "onboarding@resend.dev",
                "https://api.resend.com/emails", 5000, objectMapper, mockClient
        );

        EmailSendResult result = provider.sendEmail("user@example.com", "Test", "<p>Hi</p>", "Hi");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).doesNotContain(secretApiKey);
    }

    @Test
    @DisplayName("9. EmailProviderFactory resolves RESEND when configured")
    void testFactoryResolvesResend() {
        ResendEmailProvider resendProvider = new ResendEmailProvider(
                "re_test_key", "onboarding@resend.dev",
                "https://api.resend.com/emails", 5000, objectMapper, mock(HttpClient.class)
        );
        SimulationEmailProvider simProvider = new SimulationEmailProvider();

        EmailProviderFactory factory = new EmailProviderFactory(
                List.of(simProvider, resendProvider),
                "RESEND"
        );

        assertThat(factory.getActiveProviderName()).isEqualTo("RESEND");
        assertThat(factory.getProvider()).isSameAs(resendProvider);
        assertThat(factory.getProvider("RESEND")).isSameAs(resendProvider);
        assertThat(factory.getProvider("SIMULATION")).isSameAs(simProvider);
    }
}
