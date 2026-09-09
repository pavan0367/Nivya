package com.nivya.common.controller;

import com.nivya.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Remote Configuration & Feature Flags REST Controller.
 * Provides client version enforcement, dynamic feature toggles, and operational parameters.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "App Configuration & Remote Config", description = "Endpoints for version support, feature flags, and remote parameters")
public class AppConfigController {

    @Value("${nivya.app.min-client-version:1.0.0}")
    private String minSupportedClientVersion;

    @Value("${nivya.app.latest-client-version:1.0.0}")
    private String latestClientVersion;

    @Value("${nivya.app.force-update:false}")
    private boolean forceUpdate;

    @Value("${nivya.convocation.view-duration-seconds:120}")
    private int convocationViewDurationSeconds;

    @Value("${nivya.pairing.code-ttl-minutes:10}")
    private int pairingCodeTtlMinutes;

    @Operation(summary = "Get App Remote Configuration & Feature Flags",
               description = "Returns minimum client versions, dynamic feature toggles, and remote operational configuration")
    @GetMapping({"/app/config", "/config"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAppConfig() {
        Map<String, Object> config = new LinkedHashMap<>();

        // Versioning and update flags
        config.put("minSupportedClientVersion", minSupportedClientVersion);
        config.put("latestClientVersion", latestClientVersion);
        config.put("forceUpdateRequired", forceUpdate);

        // Feature flags
        Map<String, Boolean> featureFlags = new LinkedHashMap<>();
        featureFlags.put("convocation_enabled", true);
        featureFlags.put("battery_monitoring", true);
        featureFlags.put("network_quality", true);
        featureFlags.put("screen_time", true);
        featureFlags.put("location_tracking", true);
        featureFlags.put("clean_up", true);
        featureFlags.put("live_telemetry", true);
        featureFlags.put("alerts_enabled", true);
        config.put("featureFlags", featureFlags);

        // Remote operational parameters
        Map<String, Object> remoteConfig = new LinkedHashMap<>();
        remoteConfig.put("heartbeat_interval_seconds", 60);
        remoteConfig.put("telemetry_sync_period_minutes", 15);
        remoteConfig.put("convocation_view_duration_seconds", convocationViewDurationSeconds);
        remoteConfig.put("pairing_code_ttl_minutes", pairingCodeTtlMinutes);
        remoteConfig.put("support_email", "support@nivya.local");
        config.put("remoteConfig", remoteConfig);

        return ResponseEntity.ok(ApiResponse.success(config, "Application configuration retrieved"));
    }
}
