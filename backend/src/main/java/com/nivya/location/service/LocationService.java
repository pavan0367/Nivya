package com.nivya.location.service;

import com.nivya.alerts.service.AlertService;

import com.nivya.common.exception.ResourceNotFoundException;
import com.nivya.consent.entity.Consent;
import com.nivya.consent.repository.ConsentRepository;
import com.nivya.device.entity.Device;
import com.nivya.device.entity.DeviceStatus;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.device.repository.DeviceStatusRepository;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.location.dto.LocationHistoryResponse;
import com.nivya.location.dto.LocationPointDto;
import com.nivya.location.dto.LocationStatusResponse;
import com.nivya.location.dto.LocationTelemetryRequest;
import com.nivya.location.entity.LocationHistory;
import com.nivya.location.entity.LocationStatus;
import com.nivya.location.repository.LocationHistoryRepository;
import com.nivya.location.repository.LocationStatusRepository;
import com.nivya.security.authorization.DeviceAccessValidator;
import com.nivya.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private final LocationStatusRepository locationStatusRepository;
    private final LocationHistoryRepository locationHistoryRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceStatusRepository deviceStatusRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final ConsentRepository consentRepository;
    private final AlertService alertService;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.nivya.websocket.service.RealtimeBroadcastService realtimeBroadcastService;
    private final DeviceAccessValidator deviceAccessValidator;

    public LocationService(LocationStatusRepository locationStatusRepository,
                           LocationHistoryRepository locationHistoryRepository,
                           DeviceRepository deviceRepository,
                           DeviceStatusRepository deviceStatusRepository,
                           FamilyMemberRepository familyMemberRepository,
                           ConsentRepository consentRepository,
                           AlertService alertService,
                           SimpMessagingTemplate messagingTemplate,
                           com.nivya.websocket.service.RealtimeBroadcastService realtimeBroadcastService,
                           DeviceAccessValidator deviceAccessValidator) {
        this.locationStatusRepository = locationStatusRepository;
        this.locationHistoryRepository = locationHistoryRepository;
        this.deviceRepository = deviceRepository;
        this.deviceStatusRepository = deviceStatusRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.consentRepository = consentRepository;
        this.alertService = alertService;
        this.messagingTemplate = messagingTemplate;
        this.realtimeBroadcastService = realtimeBroadcastService;
        this.deviceAccessValidator = deviceAccessValidator;
    }

    @Transactional
    public LocationStatusResponse recordLocation(LocationTelemetryRequest request, UserPrincipal principal) {
        Device device = deviceRepository.findByDeviceUuid(request.getDeviceUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with UUID: " + request.getDeviceUuid()));

        validateDeviceAccess(device, principal);

        Instant recordedAt = request.getRecordedAt() != null ? request.getRecordedAt() : Instant.now();
        boolean isStale = Duration.between(recordedAt, Instant.now()).toMinutes() > 15;

        // 1. Update or create LocationStatus
        LocationStatus status = locationStatusRepository.findByDeviceId(device.getId())
                .orElse(new LocationStatus());
        status.setDevice(device);
        status.setLatitude(request.getLatitude());
        status.setLongitude(request.getLongitude());
        status.setAccuracyMeters(request.getAccuracyMeters());
        status.setAltitudeMeters(request.getAltitudeMeters());
        status.setSpeedMetersPerSec(request.getSpeedMetersPerSec());
        status.setBearingDegrees(request.getBearingDegrees());
        status.setProvider(request.getProvider() != null ? request.getProvider() : "gps");
        status.setGpsAvailable(Boolean.TRUE.equals(request.getIsGpsAvailable()));
        status.setNetworkAvailable(Boolean.TRUE.equals(request.getIsNetworkAvailable()));
        status.setPermissionState(request.getPermissionState() != null ? request.getPermissionState() : "GRANTED");
        status.setBackgroundConsented(Boolean.TRUE.equals(request.getIsBackgroundConsented()));
        status.setStale(isStale);
        status.setRecordedAt(recordedAt);

        status = locationStatusRepository.save(status);

        // 2. Check if location history is consented
        boolean isConsented = checkLocationConsent(device);
        if (isConsented) {
            // Apply stationary noise filter: suppress duplicate stationary points (< 15 meters within 5 minutes)
            boolean shouldSaveHistory = true;
            Optional<LocationHistory> lastPointOpt = locationHistoryRepository.findTopByDeviceIdOrderByRecordedAtDesc(device.getId());
            if (lastPointOpt.isPresent()) {
                LocationHistory last = lastPointOpt.get();
                long secondsDiff = Math.abs(Duration.between(last.getRecordedAt(), recordedAt).getSeconds());
                double distanceMeters = calculateHaversineDistance(
                        last.getLatitude(), last.getLongitude(),
                        request.getLatitude(), request.getLongitude()
                );
                if (secondsDiff < 300 && distanceMeters < 15.0) {
                    shouldSaveHistory = false;
                }
            }

            if (shouldSaveHistory) {
                LocationHistory history = new LocationHistory(
                        device,
                        request.getLatitude(),
                        request.getLongitude(),
                        request.getAccuracyMeters(),
                        request.getAltitudeMeters(),
                        request.getSpeedMetersPerSec(),
                        request.getProvider(),
                        request.getSourceMode() != null ? request.getSourceMode() : "FOREGROUND",
                        recordedAt
                );
                locationHistoryRepository.save(history);
            }
        }

        // 3. Update DeviceStatus lastSyncAt
        Optional<DeviceStatus> devStatusOpt = deviceStatusRepository.findByDeviceId(device.getId());
        if (devStatusOpt.isPresent()) {
            DeviceStatus devStatus = devStatusOpt.get();
            devStatus.setLastSyncAt(Instant.now());
            deviceStatusRepository.save(devStatus);
        }

        // 3b. Stale Location Alert Handling
        if (device.getFamily() != null) {
            boolean gpsAvail = Boolean.TRUE.equals(request.getIsGpsAvailable());
            if (!gpsAvail || isStale) {
                String devName = device.getDeviceName() != null ? device.getDeviceName() : "Device";
                String reason = !gpsAvail ? "GPS is disabled" : "No location updates in >15m";
                alertService.triggerOrUpdateAlert(
                        device.getFamily(),
                        device,
                        "STALE_LOCATION",
                        "WARNING",
                        "Stale Location Alert",
                        devName + " location is stale (" + reason + "). Showing last known coordinates.",
                        "PARENT"
                );
            } else {
                alertService.resolveAlert(device.getId(), "STALE_LOCATION");
            }
        }

        LocationStatusResponse response = mapToStatusResponse(status, device);

        // 4. WebSocket Broadcast
        if (device.getFamily() != null) {
            try {
                messagingTemplate.convertAndSend("/topic/family/" + device.getFamily().getId() + "/location", response);
                messagingTemplate.convertAndSend("/topic/device/" + device.getId() + "/location", response);
                messagingTemplate.convertAndSend("/topic/location/" + device.getId(), response);
            } catch (Exception e) {
                log.warn("Failed to broadcast WebSocket location update: {}", e.getMessage());
            }

            // Real-time broadcast service (Redis PubSub + transient store)
            realtimeBroadcastService.broadcastLocationUpdate(device.getId(), device.getFamily().getId(), response);
        } else {
            realtimeBroadcastService.broadcastLocationUpdate(device.getId(), null, response);
        }

        log.info("Recorded location for device {}: lat={}, lng={}, provider={}, isStale={}",
                device.getDeviceUuid(), status.getLatitude(), status.getLongitude(), status.getProvider(), status.isStale());

        return response;
    }

    @Transactional(readOnly = true)
    public LocationStatusResponse getCurrentLocation(Long deviceId, UserPrincipal principal) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        validateDeviceAccess(device, principal);

        LocationStatus status = locationStatusRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("No location data recorded for device: " + deviceId));

        return mapToStatusResponse(status, device);
    }

    @Transactional(readOnly = true)
    public LocationHistoryResponse getLocationHistory(Long deviceId, Instant startTime, Instant endTime, UserPrincipal principal) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        validateDeviceAccess(device, principal);

        boolean isConsented = checkLocationConsent(device);
        if (!isConsented) {
            return new LocationHistoryResponse(
                    deviceId,
                    device.getDeviceUuid(),
                    List.of(),
                    0,
                    false,
                    "Location history sharing is disabled by family consent settings."
            );
        }

        Instant start = startTime != null ? startTime : Instant.now().minus(Duration.ofHours(24));
        Instant end = endTime != null ? endTime : Instant.now();

        List<LocationHistory> historyList = locationHistoryRepository
                .findByDeviceIdAndRecordedAtBetweenOrderByRecordedAtAsc(deviceId, start, end);

        List<LocationPointDto> pointDtos = historyList.stream()
                .map(h -> new LocationPointDto(
                        h.getLatitude(),
                        h.getLongitude(),
                        h.getAccuracyMeters(),
                        h.getProvider(),
                        h.getSourceMode(),
                        h.getRecordedAt()
                ))
                .collect(Collectors.toList());

        return new LocationHistoryResponse(
                deviceId,
                device.getDeviceUuid(),
                pointDtos,
                pointDtos.size(),
                true,
                "Location history retrieved successfully."
        );
    }

    @Transactional
    public int purgeOldLocations(int retentionDays) {
        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        int deleted = locationHistoryRepository.deleteByRecordedAtBefore(cutoff);
        log.info("Purged {} location history records older than {} days (cutoff: {})", deleted, retentionDays, cutoff);
        return deleted;
    }

    private boolean checkLocationConsent(Device device) {
        if (device.getUser() == null) return false;
        List<Consent> consents = consentRepository.findByUserId(device.getUser().getId());
        if (consents.isEmpty()) {
            return true; // Default consent when family is newly created
        }
        return consents.stream().anyMatch(Consent::isLocationConsent);
    }

    private LocationStatusResponse mapToStatusResponse(LocationStatus status, Device device) {
        long minutesAgo = Duration.between(status.getRecordedAt(), Instant.now()).toMinutes();
        boolean isStale = minutesAgo > 15 || !status.isGpsAvailable();

        String staleDescription;
        if (!status.isGpsAvailable()) {
            staleDescription = "GPS is disabled on device — showing last known coordinates";
        } else if (minutesAgo > 15) {
            staleDescription = "Device has not reported location in " + minutesAgo + "m — showing last known coordinates";
        } else {
            staleDescription = null;
        }

        String lastUpdateAgo = formatTimeAgo(status.getRecordedAt());

        return new LocationStatusResponse(
                device.getId(),
                device.getDeviceUuid(),
                device.getDeviceName(),
                status.getLatitude(),
                status.getLongitude(),
                status.getAccuracyMeters(),
                status.getAltitudeMeters(),
                status.getSpeedMetersPerSec(),
                status.getBearingDegrees(),
                status.getProvider(),
                status.isGpsAvailable(),
                status.isNetworkAvailable(),
                status.getPermissionState(),
                status.isBackgroundConsented(),
                isStale,
                staleDescription,
                status.getRecordedAt(),
                status.getUpdatedAt(),
                lastUpdateAgo
        );
    }

    private String formatTimeAgo(Instant time) {
        long seconds = Duration.between(time, Instant.now()).getSeconds();
        if (seconds < 60) return "Just now";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        return (hours / 24) + "d ago";
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Radius of the Earth in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void validateDeviceAccess(Device device, UserPrincipal principal) {
        if (principal == null) return;
        deviceAccessValidator.validateDeviceAccess(device, principal);
    }
}
