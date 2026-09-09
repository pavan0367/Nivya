package com.nivya.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration & MockMvc Test for Health & Operational Endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/health should return UP status with metadata")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Service is operational"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.application").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /api/v1/ping should return pong")
    void testPingEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/ping")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("pong"));
    }

    @Test
    @DisplayName("GET /api/v1/app/config should return client version, feature flags, and remote config without auth")
    void testAppConfigEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/app/config")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.minSupportedClientVersion").value("1.0.0"))
                .andExpect(jsonPath("$.data.latestClientVersion").value("1.0.0"))
                .andExpect(jsonPath("$.data.forceUpdateRequired").value(false))
                .andExpect(jsonPath("$.data.featureFlags.convocation_enabled").value(true))
                .andExpect(jsonPath("$.data.featureFlags.battery_monitoring").value(true))
                .andExpect(jsonPath("$.data.remoteConfig.convocation_view_duration_seconds").value(120))
                .andExpect(jsonPath("$.data.remoteConfig.heartbeat_interval_seconds").value(60));
    }

    @Test
    @DisplayName("GET /api/v1/config should return same configuration payload")
    void testConfigAliasEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/config")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.minSupportedClientVersion").value("1.0.0"));
    }
}
