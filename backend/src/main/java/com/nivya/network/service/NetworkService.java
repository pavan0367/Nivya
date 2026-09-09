package com.nivya.network.service;

import com.nivya.alerts.service.AlertService;

import com.nivya.common.exception.ResourceNotFoundException;
import com.nivya.device.entity.Device;
import com.nivya.device.entity.DeviceStatus;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.device.repository.DeviceStatusRepository;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.network.dto.NetworkHistoryResponse;
import com.nivya.network.dto.NetworkStatusResponse;
import com.nivya.network.dto.NetworkTelemetryRequest;
import com.nivya.network.dto.NetworkUpdateEvent;
import com.nivya.network.entity.NetworkHistory;
import com.nivya.network.entity.NetworkStatus;
import com.nivya.network.repository.NetworkHistoryRepository;
import com.nivya.network.repository.NetworkStatusRepository;
import com.nivya.security.authorization.DeviceAccessValidator;
import com.nivya.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NetworkService {

    private static final Logger log = LoggerFactory.getLogger(NetworkService.class);

    private final NetworkStatusRepository networkStatusRepository;
    private final NetworkHistoryRepository networkHistoryRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceStatusRepository deviceStatusRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final AlertService alertService;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.nivya.websocket.service.RealtimeBroadcastService realtimeBroadcastService;
    private final DeviceAccessValidator deviceAccessValidator;

    public NetworkService(NetworkStatusRepository networkStatusRepository,
                          NetworkHistoryRepository networkHistoryRepository,
                          DeviceRepository deviceRepository,
                          DeviceStatusRepository deviceStatusRepository,
                          FamilyMemberRepository familyMemberRepository,
                          AlertService alertService,
                          SimpMessagingTemplate messagingTemplate,
                          com.nivya.websocket.service.RealtimeBroadcastService realtimeBroadcastService,
                          DeviceAccessValidator deviceAccessValidator) {
        this.networkStatusRepository = networkStatusRepository;
        this.networkHistoryRepository = networkHistoryRepository;
        this.deviceRepository = deviceRepository;
        this.deviceStatusRepository = deviceStatusRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.alertService = alertService;
        this.messagingTemplate = messagingTemplate;
        this.realtimeBroadcastService = realtimeBroadcastService;
        this.deviceAccessValidator = deviceAccessValidator;
    }

    @Transactional
    public NetworkStatusResponse recordTelemetry(NetworkTelemetryRequest request, UserPrincipal principal) {
        Device device = deviceRepository.findByDeviceUuid(request.getDeviceUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with UUID: " + request.getDeviceUuid()));

        validateDeviceAccess(device, principal);

        String netType = request.getNetworkType() != null ? request.getNetworkType().toUpperCase() : "NONE";
        String connType = request.getConnectionType();
        boolean isNetAvail = Boolean.TRUE.equals(request.getIsNetworkAvailable());
        boolean isInternetAvail = Boolean.TRUE.equals(request.getIsInternetAvailable());
        String quality = request.getQuality() != null ? request.getQuality().toUpperCase() : "UNAVAILABLE";

        // 1. Update or create NetworkStatus
        NetworkStatus status = networkStatusRepository.findByDeviceId(device.getId())
                .orElse(new NetworkStatus());
        status.setDevice(device);
        status.setNetworkType(netType);
        status.setConnectionType(connType);
        status.setNetworkAvailable(isNetAvail);
        status.setInternetAvailable(isInternetAvail);
        status.setSignalLevel(request.getSignalLevel());
        status.setSignalDbm(request.getSignalDbm());
        status.setQuality(quality);
        status.setSsid(request.getSsid());
        status.setIpAddress(request.getIpAddress());
        status.setUpdatedAt(Instant.now());
        networkStatusRepository.save(status);

        // 2. Synchronize overarching DeviceStatus (online status, network type, network quality)
        Optional<DeviceStatus> deviceStatusOpt = deviceStatusRepository.findByDeviceId(device.getId());
        boolean wasOffline = deviceStatusOpt.isPresent() && !deviceStatusOpt.get().isOnline();
        if (deviceStatusOpt.isPresent()) {
            DeviceStatus ds = deviceStatusOpt.get();
            ds.setOnline(isNetAvail && isInternetAvail);
            ds.setNetworkType(netType);
            ds.setNetworkQuality(quality);
            ds.setLastSyncAt(Instant.now());
            deviceStatusRepository.save(ds);
        }

        // 2b. Handle Offline / Online Alert transitions
        if (device.getFamily() != null) {
            boolean isOnline = isNetAvail && isInternetAvail;
            String devName = device.getDeviceName() != null ? device.getDeviceName() : "Device";
            if (!isOnline) {
                alertService.triggerOrUpdateAlert(
                        device.getFamily(),
                        device,
                        "OFFLINE",
                        "WARNING",
                        "Device Offline",
                        devName + " has lost network connection.",
                        "PARENT"
                );
            } else {
                alertService.resolveAlert(device.getId(), "OFFLINE");
                if (wasOffline) {
                    alertService.triggerOrUpdateAlert(
                            device.getFamily(),
                            device,
                            "RECONNECTED",
                            "INFO",
                            "Device Reconnected",
                            devName + " has reconnected to the network.",
                            "PARENT"
                    );
                }
            }
        }

        // 3. Append to NetworkHistory
        Instant recordedTime = request.getRecordedAt() != null ? request.getRecordedAt() : Instant.now();
        NetworkHistory history = new NetworkHistory(
                device, netType, connType, isNetAvail, isInternetAvail,
                request.getSignalLevel(), quality, recordedTime
        );
        networkHistoryRepository.save(history);

        // 4. Broadcast live WebSocket update
        broadcastNetworkEvent(device, status);

        log.info("Recorded network telemetry for device {}: type={}, quality={}, online={}",
                device.getDeviceUuid(), netType, quality, (isNetAvail && isInternetAvail));

        return toResponse(status);
    }

    @Transactional(readOnly = true)
    public NetworkStatusResponse getCurrentNetwork(Long deviceId, UserPrincipal principal) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        validateDeviceAccess(device, principal);

        NetworkStatus status = networkStatusRepository.findByDeviceId(deviceId)
                .orElseGet(() -> new NetworkStatus(device, "NONE", "Offline", false, false, 0, null, "UNAVAILABLE", null, null));

        return toResponse(status);
    }

    @Transactional(readOnly = true)
    public NetworkHistoryResponse getNetworkHistory(Long deviceId, UserPrincipal principal) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        validateDeviceAccess(device, principal);

        List<NetworkHistory> history = networkHistoryRepository.findTop50ByDeviceIdOrderByRecordedAtDesc(deviceId);
        List<NetworkHistoryResponse.NetworkPoint> points = history.stream()
                .map(h -> new NetworkHistoryResponse.NetworkPoint(
                        h.getNetworkType(),
                        h.getConnectionType(),
                        h.isNetworkAvailable(),
                        h.isInternetAvailable(),
                        h.getSignalLevel(),
                        h.getQuality(),
                        h.getRecordedAt()
                ))
                .collect(Collectors.toList());

        return new NetworkHistoryResponse(device.getId(), device.getDeviceUuid(), points);
    }

    private void broadcastNetworkEvent(Device device, NetworkStatus status) {
        NetworkUpdateEvent event = new NetworkUpdateEvent(
                device.getId(),
                device.getDeviceUuid(),
                status.getNetworkType(),
                status.getConnectionType(),
                status.isNetworkAvailable(),
                status.isInternetAvailable(),
                status.getSignalLevel(),
                status.getSignalDbm(),
                status.getQuality(),
                status.getUpdatedAt()
        );

        // Push to device-specific topic
        messagingTemplate.convertAndSend("/topic/device/" + device.getId() + "/network", event);
        messagingTemplate.convertAndSend("/topic/network/" + device.getId(), toResponse(status));

        // If part of family, broadcast to family-wide topic for parents
        if (device.getFamily() != null) {
            messagingTemplate.convertAndSend("/topic/family/" + device.getFamily().getId() + "/network", event);
        }

        // Broadcast via Redis Pub/Sub & transient store
        realtimeBroadcastService.broadcastNetworkUpdate(
                device.getId(),
                device.getFamily() != null ? device.getFamily().getId() : null,
                toResponse(status),
                status.isNetworkAvailable() && status.isInternetAvailable()
        );
    }

    private void validateDeviceAccess(Device device, UserPrincipal principal) {
        if (principal == null) return;
        deviceAccessValidator.validateDeviceAccess(device, principal);
    }

    private NetworkStatusResponse toResponse(NetworkStatus status) {
        return new NetworkStatusResponse(
                status.getDevice().getId(),
                status.getDevice().getDeviceUuid(),
                status.getDevice().getDeviceName(),
                status.getNetworkType(),
                status.getConnectionType(),
                status.isNetworkAvailable(),
                status.isInternetAvailable(),
                status.getSignalLevel(),
                status.getSignalDbm(),
                status.getQuality(),
                status.getSsid(),
                status.getIpAddress(),
                status.getUpdatedAt()
        );
    }
}
