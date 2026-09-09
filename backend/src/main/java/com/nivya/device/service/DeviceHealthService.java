package com.nivya.device.service;

import com.nivya.alerts.service.AlertService;
import com.nivya.common.exception.ResourceNotFoundException;
import com.nivya.device.dto.*;
import com.nivya.device.entity.Device;
import com.nivya.device.entity.DeviceHealth;
import com.nivya.device.entity.DeviceStatus;
import com.nivya.device.repository.DeviceHealthRepository;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.device.repository.DeviceStatusRepository;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.security.UserPrincipal;
import org.slf4j.Logger;
import com.nivya.security.authorization.DeviceAccessValidator;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class DeviceHealthService {

    private static final Logger log = LoggerFactory.getLogger(DeviceHealthService.class);

    private final DeviceHealthRepository deviceHealthRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceStatusRepository deviceStatusRepository;
    private final AlertService alertService;
    private final FamilyMemberRepository familyMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DeviceAccessValidator deviceAccessValidator;

    public DeviceHealthService(DeviceHealthRepository deviceHealthRepository,
                               DeviceRepository deviceRepository,
                               DeviceStatusRepository deviceStatusRepository,
                               AlertService alertService,
                               FamilyMemberRepository familyMemberRepository,
                               SimpMessagingTemplate messagingTemplate,
                               DeviceAccessValidator deviceAccessValidator) {
        this.deviceHealthRepository = deviceHealthRepository;
        this.deviceRepository = deviceRepository;
        this.deviceStatusRepository = deviceStatusRepository;
        this.alertService = alertService;
        this.familyMemberRepository = familyMemberRepository;
        this.messagingTemplate = messagingTemplate;
        this.deviceAccessValidator = deviceAccessValidator;
    }

    @Transactional
    public DeviceHealthResponse recordTelemetry(DeviceHealthTelemetryRequest request, UserPrincipal principal) {
        Device device = deviceRepository.findByDeviceUuid(request.getDeviceUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with UUID: " + request.getDeviceUuid()));

        validateDeviceAccess(device, principal);

        // 1. Evaluate Storage Metrics
        long totalStorage = request.getStorageTotalBytes() != null ? request.getStorageTotalBytes() : 0L;
        long usedStorage = request.getStorageUsedBytes() != null ? request.getStorageUsedBytes() : 0L;
        long freeStorage = request.getStorageFreeBytes() != null ? request.getStorageFreeBytes()
                : Math.max(0L, totalStorage - usedStorage);
        double storageUsedPct = totalStorage > 0 ? (double) usedStorage / totalStorage * 100.0 : 0.0;
        boolean isLowStorage = totalStorage > 0 && (freeStorage < (totalStorage * 0.10) || freeStorage < 2_000_000_000L); // <10% or <2GB

        // 2. Evaluate Memory (RAM) Metrics
        long totalRam = request.getRamTotalBytes() != null ? request.getRamTotalBytes() : 0L;
        long usedRam = request.getRamUsedBytes() != null ? request.getRamUsedBytes() : 0L;
        long freeRam = request.getRamFreeBytes() != null ? request.getRamFreeBytes()
                : Math.max(0L, totalRam - usedRam);
        boolean isLowRam = Boolean.TRUE.equals(request.getIsLowRam()) || (totalRam > 0 && freeRam < (totalRam * 0.10));

        // 3. Evaluate Permission Health
        String locPerm = request.getLocationPermission() != null ? request.getLocationPermission().toUpperCase() : "GRANTED";
        String usagePerm = request.getUsagePermission() != null ? request.getUsagePermission().toUpperCase() : "GRANTED";
        String notifPerm = request.getNotificationPermission() != null ? request.getNotificationPermission().toUpperCase() : "GRANTED";
        String battOpt = request.getBatteryOptimization() != null ? request.getBatteryOptimization().toUpperCase() : "OPTIMIZED";

        boolean locHealthy = locPerm.contains("GRANTED");
        boolean usageHealthy = "GRANTED".equals(usagePerm);
        boolean notifHealthy = notifPerm.contains("GRANTED");
        boolean allPermissionsHealthy = locHealthy && usageHealthy && notifHealthy;

        // 4. Compute Comprehensive Health Score (0 - 100)
        int score = 100;
        if (totalStorage > 0 && freeStorage < (totalStorage * 0.05)) {
            score -= 30; // Critical storage
        } else if (isLowStorage) {
            score -= 15;
        }

        if (isLowRam) {
            score -= 15;
        }

        int batteryPct = request.getBatteryPct() != null ? request.getBatteryPct() : 100;
        String chargingState = request.getChargingState() != null ? request.getChargingState().toUpperCase() : "NOT_CHARGING";
        boolean isCharging = chargingState.contains("CHARGING") || chargingState.contains("FULL");
        if (batteryPct <= 15 && !isCharging) {
            score -= 20;
        } else if (batteryPct <= 25 && !isCharging) {
            score -= 10;
        }

        if (!locHealthy) score -= 15;
        if (!usageHealthy) score -= 15;
        if (!notifHealthy) score -= 10;

        score = Math.max(10, Math.min(100, score));

        String healthStatus;
        if (score >= 85) {
            healthStatus = "EXCELLENT";
        } else if (score >= 70) {
            healthStatus = "HEALTHY";
        } else if (score >= 50) {
            healthStatus = "WARNING";
        } else {
            healthStatus = "CRITICAL";
        }

        // 5. Persist or Update DeviceHealth Record
        DeviceHealth health = deviceHealthRepository.findByDeviceId(device.getId())
                .orElse(new DeviceHealth(device, request.getRecordedAt()));

        health.setDevice(device);
        health.setBatteryPct(request.getBatteryPct());
        health.setChargingState(request.getChargingState());
        health.setBatteryHealth(request.getBatteryHealth() != null ? request.getBatteryHealth() : "GOOD");
        health.setBatteryTempCelsius(request.getBatteryTempCelsius());
        health.setStorageTotalBytes(totalStorage);
        health.setStorageUsedBytes(usedStorage);
        health.setStorageFreeBytes(freeStorage);
        health.setRamTotalBytes(totalRam);
        health.setRamUsedBytes(usedRam);
        health.setRamFreeBytes(freeRam);
        health.setLowRam(isLowRam);
        health.setDeviceModel(request.getDeviceModel());
        health.setDeviceManufacturer(request.getDeviceManufacturer());
        health.setOsVersion(request.getOsVersion());
        health.setSdkVersion(request.getSdkVersion());
        health.setNetworkType(request.getNetworkType());
        health.setOnline(request.getIsOnline() == null || request.getIsOnline());
        health.setLocationPermission(locPerm);
        health.setUsagePermission(usagePerm);
        health.setNotificationPermission(notifPerm);
        health.setBatteryOptimization(battOpt);
        health.setAllPermissionsHealthy(allPermissionsHealthy);
        health.setHealthScore(score);
        health.setHealthStatus(healthStatus);
        health.setSyncState(request.getSyncState() != null ? request.getSyncState() : "SYNCED");
        health.setRecordedAt(request.getRecordedAt() != null ? request.getRecordedAt() : Instant.now());
        health.setUpdatedAt(Instant.now());

        DeviceHealth savedHealth = deviceHealthRepository.save(health);

        // 6. Update overarching DeviceStatus
        Optional<DeviceStatus> statusOpt = deviceStatusRepository.findByDeviceId(device.getId());
        if (statusOpt.isPresent()) {
            DeviceStatus ds = statusOpt.get();
            if (request.getBatteryPct() != null) {
                ds.setBatteryPct(request.getBatteryPct());
            }
            if (request.getNetworkType() != null) {
                ds.setNetworkType(request.getNetworkType());
            }
            if (request.getIsOnline() != null) {
                ds.setOnline(request.getIsOnline());
            }
            ds.setLastSyncAt(Instant.now());
            deviceStatusRepository.save(ds);
        }

        // 7. Update device OS version if detected
        if (request.getOsVersion() != null && !request.getOsVersion().isBlank()) {
            device.setOsVersion(request.getOsVersion());
            deviceRepository.save(device);
        }

        // 8. Low Storage Alert Handling
        handleStorageAlert(device, isLowStorage, freeStorage, totalStorage);

        // 9. Permission Health Alert Handling
        handlePermissionAlert(device, allPermissionsHealthy, locHealthy, usageHealthy, notifHealthy);

        // 10. Map and Broadcast via WebSocket
        DeviceHealthResponse response = mapToResponse(savedHealth, device);
        try {
            messagingTemplate.convertAndSend("/topic/device-health/" + device.getId(), response);
        } catch (Exception e) {
            log.warn("WebSocket publish failed for device health {}: {}", device.getId(), e.getMessage());
        }

        return response;
    }

    @Transactional(readOnly = true)
    public DeviceHealthResponse getDeviceHealth(Long deviceId, UserPrincipal principal) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));

        validateDeviceAccess(device, principal);

        DeviceHealth health = deviceHealthRepository.findByDeviceId(deviceId)
                .orElseGet(() -> createDefaultHealth(device));

        return mapToResponse(health, device);
    }

    @Transactional(readOnly = true)
    public DeviceHealthResponse getMyDeviceHealth(UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }

        Device device = deviceRepository.findFirstByUserIdOrderByIdAsc(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No enrolled device found for current user"));

        return getDeviceHealth(device.getId(), principal);
    }

    private void handleStorageAlert(Device device, boolean isLowStorage, long freeStorage, long totalStorage) {
        if (device.getFamily() == null) return;
        if (isLowStorage) {
            long freeGb = freeStorage / (1024 * 1024 * 1024);
            String severity = freeStorage < (totalStorage * 0.05) ? "CRITICAL" : "WARNING";
            String devName = device.getDeviceName() != null ? device.getDeviceName() : "Device";
            alertService.triggerOrUpdateAlert(
                    device.getFamily(),
                    device,
                    "LOW_STORAGE",
                    severity,
                    "Low Storage Warning",
                    devName + " is running low on storage (" + freeGb + " GB free). Consider freeing up space.",
                    "ALL"
            );
        } else {
            alertService.resolveAlert(device.getId(), "LOW_STORAGE");
        }
    }

    private void handlePermissionAlert(Device device, boolean allHealthy, boolean locHealthy, boolean usageHealthy, boolean notifHealthy) {
        if (device.getFamily() == null) return;
        if (!allHealthy) {
            String devName = device.getDeviceName() != null ? device.getDeviceName() : "Device";
            StringBuilder sb = new StringBuilder("Required permissions are disabled on ").append(devName).append(": ");
            if (!locHealthy) sb.append("Location, ");
            if (!usageHealthy) sb.append("Usage Access, ");
            if (!notifHealthy) sb.append("Notifications, ");
            String msg = sb.substring(0, sb.length() - 2);

            alertService.triggerOrUpdateAlert(
                    device.getFamily(),
                    device,
                    "PERMISSION_REVOKED",
                    "CRITICAL",
                    "Permission Attention Needed",
                    msg,
                    "ALL"
            );
        } else {
            alertService.resolveAlert(device.getId(), "PERMISSION_REVOKED");
        }
    }

    private void validateDeviceAccess(Device device, UserPrincipal principal) {
        if (principal == null) return;
        deviceAccessValidator.validateDeviceAccess(device, principal);
    }

    private DeviceHealth createDefaultHealth(Device device) {
        DeviceHealth defaultHealth = new DeviceHealth(device, Instant.now());
        defaultHealth.setHealthScore(95);
        defaultHealth.setHealthStatus("HEALTHY");
        defaultHealth.setBatteryPct(85);
        defaultHealth.setChargingState("NOT_CHARGING");
        defaultHealth.setBatteryHealth("GOOD");
        defaultHealth.setStorageTotalBytes(128_000_000_000L);
        defaultHealth.setStorageUsedBytes(48_000_000_000L);
        defaultHealth.setStorageFreeBytes(80_000_000_000L);
        defaultHealth.setRamTotalBytes(8_000_000_000L);
        defaultHealth.setRamUsedBytes(3_500_000_000L);
        defaultHealth.setRamFreeBytes(4_500_000_000L);
        defaultHealth.setLowRam(false);
        defaultHealth.setDeviceModel(device.getDeviceName());
        defaultHealth.setDeviceManufacturer("Android OEM");
        defaultHealth.setOsVersion(device.getOsVersion() != null ? device.getOsVersion() : "14");
        defaultHealth.setSdkVersion(34);
        defaultHealth.setNetworkType("WIFI");
        defaultHealth.setOnline(true);
        defaultHealth.setLocationPermission("GRANTED");
        defaultHealth.setUsagePermission("GRANTED");
        defaultHealth.setNotificationPermission("GRANTED");
        defaultHealth.setBatteryOptimization("OPTIMIZED");
        defaultHealth.setAllPermissionsHealthy(true);
        defaultHealth.setSyncState("SYNCED");
        return defaultHealth;
    }

    private DeviceHealthResponse mapToResponse(DeviceHealth health, Device device) {
        DeviceHealthResponse res = new DeviceHealthResponse();
        res.setDeviceId(device.getId());
        res.setDeviceUuid(device.getDeviceUuid());
        res.setDeviceName(device.getDeviceName());

        res.setDeviceModel(health.getDeviceModel() != null ? health.getDeviceModel() : device.getDeviceName());
        res.setDeviceManufacturer(health.getDeviceManufacturer() != null ? health.getDeviceManufacturer() : "Android");
        res.setOsVersion(health.getOsVersion() != null ? health.getOsVersion() : device.getOsVersion());
        res.setSdkVersion(health.getSdkVersion());

        // Storage
        long totalStorage = health.getStorageTotalBytes() != null ? health.getStorageTotalBytes() : 0L;
        long usedStorage = health.getStorageUsedBytes() != null ? health.getStorageUsedBytes() : 0L;
        long freeStorage = health.getStorageFreeBytes() != null ? health.getStorageFreeBytes() : 0L;
        double storageUsedPct = totalStorage > 0 ? Math.round(((double) usedStorage / totalStorage * 100.0) * 10.0) / 10.0 : 0.0;
        boolean isLowStorage = totalStorage > 0 && freeStorage < (totalStorage * 0.10);
        res.setStorage(new StorageHealthDto(totalStorage, usedStorage, freeStorage, storageUsedPct, isLowStorage));

        // Memory
        long totalRam = health.getRamTotalBytes() != null ? health.getRamTotalBytes() : 0L;
        long usedRam = health.getRamUsedBytes() != null ? health.getRamUsedBytes() : 0L;
        long freeRam = health.getRamFreeBytes() != null ? health.getRamFreeBytes() : 0L;
        double ramUsedPct = totalRam > 0 ? Math.round(((double) usedRam / totalRam * 100.0) * 10.0) / 10.0 : 0.0;
        res.setMemory(new MemoryHealthDto(totalRam, usedRam, freeRam, ramUsedPct, health.isLowRam()));

        // Battery
        res.setBatteryPct(health.getBatteryPct());
        res.setChargingState(health.getChargingState());
        res.setBatteryHealth(health.getBatteryHealth());
        res.setBatteryTempCelsius(health.getBatteryTempCelsius());

        // Connectivity & Sync
        res.setNetworkType(health.getNetworkType());
        res.setOnline(health.isOnline());
        res.setSyncState(health.getSyncState());

        // Permission Health
        PermissionHealthDto permDto = new PermissionHealthDto(
                health.getLocationPermission(),
                health.getUsagePermission(),
                health.getNotificationPermission(),
                health.getBatteryOptimization(),
                health.isAllPermissionsHealthy()
        );
        res.setPermissionHealth(permDto);

        res.setHealthScore(health.getHealthScore());
        res.setHealthStatus(health.getHealthStatus());

        // Child friendly summaries
        if (health.getHealthScore() >= 80) {
            res.setConditionSummary("Your phone is running great! 🎉");
        } else if (health.getHealthScore() >= 60) {
            res.setConditionSummary("Your phone is doing okay, with a few reminders. 👍");
        } else {
            res.setConditionSummary("Your device needs a little attention! ⚠️");
        }

        if (freeStorage > 20_000_000_000L) { // >20GB
            res.setStorageSummary("Plenty of storage available for photos, games, and apps.");
        } else if (freeStorage > 5_000_000_000L) {
            res.setStorageSummary("Storage is in good shape.");
        } else {
            res.setStorageSummary("Storage is getting tight. Time for a cleanup!");
        }

        if (health.getBatteryPct() != null) {
            if (health.getBatteryPct() > 50) {
                res.setBatterySummary("Battery level is healthy and ready to go.");
            } else if (health.getBatteryPct() > 20) {
                res.setBatterySummary("Good battery level for normal use.");
            } else {
                res.setBatterySummary("Battery is running low. Plug in your charger!");
            }
        } else {
            res.setBatterySummary("Battery status is regular.");
        }

        if (health.isAllPermissionsHealthy()) {
            res.setProtectionSummary("All Nivya protections are active and keeping you safe.");
        } else {
            res.setProtectionSummary("Some permissions need to be enabled in settings.");
        }

        res.setRecordedAt(health.getRecordedAt());
        res.setUpdatedAt(health.getUpdatedAt());

        return res;
    }
}
