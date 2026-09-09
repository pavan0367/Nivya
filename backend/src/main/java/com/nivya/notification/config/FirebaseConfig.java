package com.nivya.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Firebase Cloud Messaging initialization configuration.
 * Credentials are strictly loaded from environment configuration:
 * - FIREBASE_CREDENTIALS_PATH: Absolute path to service account JSON
 * - FIREBASE_CREDENTIALS_JSON: Raw service account JSON content
 *
 * Never hardcodes credentials or secrets. Gracefully falls back to simulation mode
 * when running in test, local development, or offline CI environments.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${nivya.firebase.enabled:true}")
    private boolean enabled;

    @Value("${nivya.firebase.credentials-path:}")
    private String credentialsPath;

    @Value("${nivya.firebase.credentials-json:}")
    private String credentialsJson;

    private boolean initialized = false;

    @PostConstruct
    public void initializeFirebase() {
        if (!enabled) {
            log.info("Firebase integration is disabled by configuration (nivya.firebase.enabled=false). Operating in simulation mode.");
            return;
        }

        if (FirebaseApp.getApps().size() > 0) {
            initialized = true;
            log.info("FirebaseApp is already initialized.");
            return;
        }

        try (InputStream serviceAccountStream = resolveCredentialsStream()) {
            if (serviceAccountStream != null) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccountStream);
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();

                FirebaseApp.initializeApp(options);
                initialized = true;
                log.info("FirebaseApp initialized successfully via environment configuration.");
            } else {
                // Try Application Default Credentials (ADC)
                try {
                    GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(credentials)
                            .build();
                    FirebaseApp.initializeApp(options);
                    initialized = true;
                    log.info("FirebaseApp initialized successfully via Application Default Credentials.");
                } catch (Exception e) {
                    log.info("No Firebase credentials provided in environment. Operating in simulation mode.");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to initialize FirebaseApp from environment credentials ({}). Operating in simulation mode.", e.getMessage());
        }
    }

    private InputStream resolveCredentialsStream() throws Exception {
        if (credentialsPath != null && !credentialsPath.isBlank()) {
            File credFile = new File(credentialsPath.trim());
            if (credFile.exists() && credFile.canRead()) {
                log.info("Loading Firebase credentials from file path: {}", credentialsPath);
                return new FileInputStream(credFile);
            } else {
                log.warn("Firebase credentials path '{}' does not exist or is not readable.", credentialsPath);
            }
        }

        if (credentialsJson != null && !credentialsJson.isBlank()) {
            log.info("Loading Firebase credentials from environment JSON string.");
            return new ByteArrayInputStream(credentialsJson.trim().getBytes(StandardCharsets.UTF_8));
        }

        return null;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
