package com.nivya.websocket.service;

import com.nivya.activity.dto.ActivityEventDto;
import com.nivya.activity.dto.LiveActivityResponse;
import com.nivya.activity.service.LiveActivityService;
import com.nivya.battery.dto.BatteryStatusResponse;
import com.nivya.battery.service.BatteryService;
import com.nivya.common.exception.ResourceNotFoundException;
import com.nivya.device.entity.Device;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.location.dto.LocationStatusResponse;
import com.nivya.location.service.LocationService;
import com.nivya.network.dto.NetworkStatusResponse;
import com.nivya.network.service.NetworkService;
import com.nivya.role.RoleType;
import com.nivya.security.UserPrincipal;
import com.nivya.websocket.dto.DeviceTelemetrySnapshotDto;
import com.nivya.websocket.redis.TransientStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Service that builds a consolidated real-time state snapshot for a device.
 * Checks Redis transient state cache first for instantaneous performance,
 * falling back to database repositories on cache miss.
 */
@Service
public class TelemetrySnapshotService {

    private static final Logger log = LoggerFactory.getLogger(TelemetrySnapshotService.class);

    private final DeviceRepository deviceRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final BatteryService batteryService;
    private final NetworkService networkService;
    private final LocationService locationService;
    private final LiveActivityService liveActivityService;
    private final TransientStateStore transientStateStore;

    public TelemetrySnapshotService(DeviceRepository deviceRepository,
                                    FamilyMemberRepository familyMemberRepository,
                                    BatteryService batteryService,
                                    NetworkService networkService,
                                    LocationService locationService,
                                    LiveActivityService liveActivityService,
                                    TransientStateStore transientStateStore) {
        this.deviceRepository = deviceRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.batteryService = batteryService;
        this.networkService = networkService;
        this.locationService = locationService;
        this.liveActivityService = liveActivityService;
        this.transientStateStore = transientStateStore;
    }

    public DeviceTelemetrySnapshotDto getDeviceSnapshot(Long deviceId, UserPrincipal principal) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceId));

        verifyAccess(device, principal);

        // Check Redis transient state store first
        DeviceTelemetrySnapshotDto cached = transientStateStore.getSnapshot(deviceId, DeviceTelemetrySnapshotDto.class);
        if (cached != null) {
            // If child is requesting, scrub parent-only activity from cached response
            if (principal.getRole() != RoleType.PARENT) {
                cached.setCurrentActivity(null);
            }
            return cached;
        }

        // Fetch components with graceful null handling
        BatteryStatusResponse battery = null;
        try {
            battery = batteryService.getCurrentBattery(deviceId, principal);
        } catch (Exception e) {
            log.debug("Battery status unavailable for snapshot: {}", e.getMessage());
        }

        NetworkStatusResponse network = null;
        try {
            network = networkService.getCurrentNetwork(deviceId, principal);
        } catch (Exception e) {
            log.debug("Network status unavailable for snapshot: {}", e.getMessage());
        }

        LocationStatusResponse location = null;
        try {
            location = locationService.getCurrentLocation(deviceId, principal);
        } catch (Exception e) {
            log.debug("Location status unavailable for snapshot: {}", e.getMessage());
        }

        ActivityEventDto currentActivity = null;
        if (principal.getRole() == RoleType.PARENT) {
            try {
                LiveActivityResponse liveResp = liveActivityService.getLiveActivity(deviceId, principal);
                if (liveResp != null) {
                    currentActivity = liveResp.getCurrentActivity();
                }
            } catch (Exception e) {
                log.debug("Live activity unavailable for snapshot: {}", e.getMessage());
            }
        }

        boolean online = transientStateStore.isDeviceOnline(deviceId);
        if (network != null && Boolean.TRUE.equals(network.getIsInternetAvailable())) {
            online = true;
        }

        DeviceTelemetrySnapshotDto snapshot = new DeviceTelemetrySnapshotDto(
                device.getId(),
                device.getDeviceName(),
                online,
                battery,
                network,
                location,
                currentActivity,
                device.getLastSeenAt()
        );

        // Cache in Redis transient store
        transientStateStore.saveSnapshot(deviceId, snapshot);

        return snapshot;
    }

    private void verifyAccess(Device device, UserPrincipal principal) {
        if (device.getUser() != null && device.getUser().getId().equals(principal.getId())) {
            return;
        }

        if (device.getFamily() != null) {
            boolean isFamilyMember = familyMemberRepository.existsByFamilyIdAndUserId(device.getFamily().getId(), principal.getId());
            if (isFamilyMember) {
                return;
            }
        }

        throw new AccessDeniedException("Unauthorized access to device telemetry snapshot: " + device.getId());
    }
}
