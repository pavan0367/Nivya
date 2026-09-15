package com.nivya.device.controller;

import com.nivya.common.exception.ResourceNotFoundException;
import com.nivya.common.response.ApiResponse;
import com.nivya.device.dto.DeviceHeartbeatRequest;
import com.nivya.device.entity.Device;
import com.nivya.device.entity.DeviceStatus;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.device.repository.DeviceStatusRepository;
import com.nivya.pairing.dto.DeviceStatusDto;
import com.nivya.security.UserPrincipal;
import com.nivya.security.authorization.DeviceAccessValidator;
import com.nivya.websocket.redis.TransientStateStore;
import com.nivya.websocket.service.RealtimeBroadcastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping({"/api/v1/devices", "/api/v1/device"})
@Tag(name = "Device Heartbeat", description = "Endpoints for device heartbeat, presence reporting, and status synchronization")
@SecurityRequirement(name = "Bearer Authentication")
public class DeviceHeartbeatController {

    private static final Logger log = LoggerFactory.getLogger(DeviceHeartbeatController.class);

    private final DeviceRepository deviceRepository;
    private final DeviceStatusRepository deviceStatusRepository;
    private final DeviceAccessValidator deviceAccessValidator;
    private final TransientStateStore transientStateStore;
    private final RealtimeBroadcastService realtimeBroadcastService;

    public DeviceHeartbeatController(DeviceRepository deviceRepository,
                                     DeviceStatusRepository deviceStatusRepository,
                                     DeviceAccessValidator deviceAccessValidator,
                                     TransientStateStore transientStateStore,
                                     RealtimeBroadcastService realtimeBroadcastService) {
        this.deviceRepository = deviceRepository;
        this.deviceStatusRepository = deviceStatusRepository;
        this.deviceAccessValidator = deviceAccessValidator;
        this.transientStateStore = transientStateStore;
        this.realtimeBroadcastService = realtimeBroadcastService;
    }

    @PostMapping("/heartbeat")
    @Transactional
    @Operation(summary = "Submit device heartbeat", description = "Updates last seen timestamp, active connectivity, and presence status")
    public ResponseEntity<ApiResponse<DeviceStatusDto>> submitHeartbeat(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) DeviceHeartbeatRequest request) {

        Device device = resolveDevice(request, principal);
        if (principal != null) {
            deviceAccessValidator.validateDeviceAccess(device, principal);
        }

        Instant now = Instant.now();
        device.setLastSeenAt(now);
        deviceRepository.save(device);

        boolean isOnline = request == null || request.getIsOnline() == null || request.getIsOnline();

        DeviceStatus status = deviceStatusRepository.findByDeviceId(device.getId())
                .orElseGet(() -> new DeviceStatus(device, isOnline, null, "UNKNOWN", "UNKNOWN"));

        status.setOnline(isOnline);
        status.setLastSyncAt(now);
        if (request != null) {
            if (request.getBatteryPct() != null) {
                status.setBatteryPct(request.getBatteryPct());
            }
            if (request.getNetworkType() != null) {
                status.setNetworkType(request.getNetworkType());
            }
            if (request.getNetworkQuality() != null) {
                status.setNetworkQuality(request.getNetworkQuality());
            }
        }
        deviceStatusRepository.save(status);

        transientStateStore.setDeviceOnline(device.getId(), isOnline);

        Long familyId = device.getFamily() != null ? device.getFamily().getId() : null;
        realtimeBroadcastService.broadcastDeviceStatus(device.getId(), familyId, isOnline, "HEARTBEAT");

        DeviceStatusDto dto = new DeviceStatusDto(
                device.getId(),
                device.getDeviceUuid(),
                device.getDeviceName(),
                device.getPlatform(),
                isOnline,
                status.getBatteryPct(),
                status.getNetworkType(),
                status.getNetworkQuality(),
                status.getLastSyncAt(),
                device.getLastSeenAt(),
                false
        );
        dto.setId(device.getId());
        if (device.getUser() != null) {
            dto.setUserId(device.getUser().getId());
            if (device.getUser().getRole() != null) {
                dto.setUserRole(device.getUser().getRole().name());
                dto.setIsChildDevice("CHILD".equalsIgnoreCase(device.getUser().getRole().name()));
            }
        }

        log.debug("Heartbeat processed for device {} (id={}): online={}", device.getDeviceUuid(), device.getId(), isOnline);
        return ResponseEntity.ok(ApiResponse.success(dto, "Device heartbeat recorded successfully"));
    }

    private Device resolveDevice(DeviceHeartbeatRequest request, UserPrincipal principal) {
        if (request != null && request.getDeviceId() != null) {
            return deviceRepository.findById(request.getDeviceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + request.getDeviceId()));
        }

        if (request != null && request.getDeviceUuid() != null && !request.getDeviceUuid().isBlank()) {
            return deviceRepository.findByDeviceUuid(request.getDeviceUuid().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Device not found with UUID: " + request.getDeviceUuid()));
        }

        if (principal != null) {
            return deviceRepository.findFirstByUserIdOrderByIdAsc(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("No enrolled device found for current user (ID: " + principal.getId() + ")"));
        }

        throw new ResourceNotFoundException("Device identification required for heartbeat");
    }
}
