package com.nivya.battery.service;

import com.nivya.alerts.service.AlertService;
import com.nivya.battery.dto.*;
import com.nivya.battery.entity.BatteryHistory;
import com.nivya.battery.entity.BatteryStatus;
import com.nivya.battery.repository.BatteryHistoryRepository;
import com.nivya.battery.repository.BatteryStatusRepository;
import com.nivya.common.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.nivya.device.entity.Device;
import com.nivya.device.entity.DeviceStatus;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.device.repository.DeviceStatusRepository;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BatteryService {

    private static final Logger log = LoggerFactory.getLogger(BatteryService.class);
    private static final int LOW_BATTERY_THRESHOLD = 20;

    private final BatteryStatusRepository batteryStatusRepository;
    private final BatteryHistoryRepository batteryHistoryRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceStatusRepository deviceStatusRepository;
    private final AlertService alertService;
    private final FamilyMemberRepository familyMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public BatteryService(BatteryStatusRepository batteryStatusRepository,
                          BatteryHistoryRepository batteryHistoryRepository,
                          DeviceRepository deviceRepository,
                          DeviceStatusRepository deviceStatusRepository,
                          AlertService alertService,
                          FamilyMemberRepository familyMemberRepository,
                          SimpMessagingTemplate messagingTemplate) {
        this.batteryStatusRepository = batteryStatusRepository;
        this.batteryHistoryRepository = batteryHistoryRepository;
        this.deviceRepository = deviceRepository;
        this.deviceStatusRepository = deviceStatusRepository;
        this.alertService = alertService;
        this.familyMemberRepository = familyMemberRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public BatteryStatusResponse recordTelemetry(BatteryTelemetryRequest request, UserPrincipal principal) {
        Device device = deviceRepository.findByDeviceUuid(request.getDeviceUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with UUID: " + request.getDeviceUuid()));

        // Security check: ensure user owns this device or belongs to the linked family
        validateDeviceAccess(device, principal);

        int pct = request.getBatteryPct();
        String chargingState = request.getChargingState() != null ? request.getChargingState().toUpperCase() : "DISCHARGING";
        String batteryState = request.getBatteryState() != null ? request.getBatteryState().toUpperCase() : "UNPLUGGED";
        String health = request.getHealth() != null ? request.getHealth().toUpperCase() : "GOOD";
        boolean isLowBattery = pct <= LOW_BATTERY_THRESHOLD && !"CHARGING".equalsIgnoreCase(chargingState);

        // 1. Update or create BatteryStatus
        BatteryStatus status = batteryStatusRepository.findByDeviceId(device.getId())
                .orElse(new BatteryStatus());
        status.setDevice(device);
        status.setBatteryPct(pct);
        status.setChargingState(chargingState);
        status.setBatteryState(batteryState);
        status.setHealth(health);
        status.setTemperatureCelsius(request.getTemperatureCelsius());
        status.setLowBattery(isLowBattery);
        status.setUpdatedAt(Instant.now());
        batteryStatusRepository.save(status);

        // 2. Synchronize DeviceStatus.batteryPct for overarching telemetry queries
        Optional<DeviceStatus> deviceStatusOpt = deviceStatusRepository.findByDeviceId(device.getId());
        if (deviceStatusOpt.isPresent()) {
            DeviceStatus ds = deviceStatusOpt.get();
            ds.setBatteryPct(pct);
            ds.setLastSyncAt(Instant.now());
            deviceStatusRepository.save(ds);
        }

        // 3. Append to BatteryHistory
        Instant recordedTime = request.getRecordedAt() != null ? request.getRecordedAt() : Instant.now();
        BatteryHistory history = new BatteryHistory(
                device, pct, chargingState, batteryState, health, request.getTemperatureCelsius(), recordedTime
        );
        batteryHistoryRepository.save(history);

        // 4. Low-Battery Safety Alert generation / resolution
        if (device.getFamily() != null) {
            handleLowBatteryAlert(device, pct, isLowBattery);

            // 5. Broadcast live WebSocket update to family and device STOMP topics
            BatteryUpdateEvent event = new BatteryUpdateEvent(
                    device.getId(), device.getDeviceUuid(), device.getFamily().getId(), pct, chargingState, isLowBattery
            );
            try {
                messagingTemplate.convertAndSend("/topic/family/" + device.getFamily().getId() + "/battery", event);
                messagingTemplate.convertAndSend("/topic/device/" + device.getId() + "/battery", event);
            } catch (Exception e) {
                log.warn("Failed to broadcast WebSocket battery update: {}", e.getMessage());
            }
        }

        return mapToResponse(status);
    }

    @Transactional(readOnly = true)
    public BatteryStatusResponse getCurrentBattery(Long deviceId, UserPrincipal principal) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));
        validateDeviceAccess(device, principal);

        BatteryStatus status = batteryStatusRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("No battery telemetry recorded for device ID: " + deviceId));

        return mapToResponse(status);
    }

    @Transactional(readOnly = true)
    public BatteryHistoryResponse getBatteryHistory(Long deviceId, UserPrincipal principal) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));
        validateDeviceAccess(device, principal);

        List<BatteryHistory> historyList = batteryHistoryRepository.findTop50ByDeviceIdOrderByRecordedAtDesc(deviceId);
        List<BatteryHistoryResponse.BatteryDataPoint> points = historyList.stream()
                .map(h -> new BatteryHistoryResponse.BatteryDataPoint(
                        h.getBatteryPct(), h.getChargingState(), h.getBatteryState(),
                        h.getTemperatureCelsius(), h.getRecordedAt()
                ))
                .collect(Collectors.toList());

        return new BatteryHistoryResponse(device.getId(), device.getDeviceUuid(), points);
    }

    @Transactional(readOnly = true)
    public BatteryTrendResponse getBatteryTrends(Long deviceId, UserPrincipal principal) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));
        validateDeviceAccess(device, principal);

        BatteryStatus currentStatus = batteryStatusRepository.findByDeviceId(deviceId)
                .orElse(null);
        int currentPct = currentStatus != null ? currentStatus.getBatteryPct() : 50;
        String chargingState = currentStatus != null ? currentStatus.getChargingState() : "DISCHARGING";

        // Query points over the last 12 hours
        Instant since = Instant.now().minus(Duration.ofHours(12));
        List<BatteryHistory> recentHistory = batteryHistoryRepository.findByDeviceIdAndRecordedAtAfterOrderByRecordedAtAsc(deviceId, since);

        double drainRate = 0.0;
        Double estimatedHours = null;
        double avgTemp = 28.0;

        if (recentHistory.size() >= 2) {
            BatteryHistory first = recentHistory.get(0);
            BatteryHistory last = recentHistory.get(recentHistory.size() - 1);
            long minutes = Duration.between(first.getRecordedAt(), last.getRecordedAt()).toMinutes();

            if (minutes > 5) {
                int drop = first.getBatteryPct() - last.getBatteryPct();
                if (drop > 0) {
                    drainRate = ((double) drop / minutes) * 60.0;
                    if (drainRate > 0) {
                        estimatedHours = currentPct / drainRate;
                    }
                }
            }

            avgTemp = recentHistory.stream()
                    .filter(h -> h.getTemperatureCelsius() != null)
                    .mapToDouble(BatteryHistory::getTemperatureCelsius)
                    .average()
                    .orElse(28.0);
        }

        return new BatteryTrendResponse(
                deviceId,
                currentPct,
                chargingState,
                Math.round(drainRate * 10.0) / 10.0,
                estimatedHours != null ? Math.round(estimatedHours * 10.0) / 10.0 : null,
                Math.round(avgTemp * 10.0) / 10.0
        );
    }

    private void handleLowBatteryAlert(Device device, int pct, boolean isLowBattery) {
        if (device.getFamily() == null) return;
        if (isLowBattery) {
            String severity = pct <= 5 ? "CRITICAL" : "WARNING";
            String devName = device.getDeviceName() != null ? device.getDeviceName() : "Device";
            alertService.triggerOrUpdateAlert(
                    device.getFamily(),
                    device,
                    "LOW_BATTERY",
                    severity,
                    "Low Battery Alert",
                    devName + " battery is low (" + pct + "%). Connect charger soon.",
                    "ALL"
            );
        } else {
            alertService.resolveAlert(device.getId(), "LOW_BATTERY");
        }
    }

    private void validateDeviceAccess(Device device, UserPrincipal principal) {
        if (principal == null) return;
        boolean isOwner = device.getUser() != null && device.getUser().getId().equals(principal.getId());
        boolean isFamilyMember = device.getFamily() != null &&
                familyMemberRepository.findByFamilyIdAndUserId(device.getFamily().getId(), principal.getId()).isPresent();

        if (!isOwner && !isFamilyMember) {
            throw new SecurityException("Unauthorized: user does not have permission to access device " + device.getId());
        }
    }

    private BatteryStatusResponse mapToResponse(BatteryStatus status) {
        return new BatteryStatusResponse(
                status.getDevice().getId(),
                status.getDevice().getDeviceUuid(),
                status.getDevice().getDeviceName(),
                status.getBatteryPct(),
                status.getChargingState(),
                status.getBatteryState(),
                status.getHealth(),
                status.getTemperatureCelsius(),
                status.isLowBattery(),
                status.getUpdatedAt()
        );
    }
}
