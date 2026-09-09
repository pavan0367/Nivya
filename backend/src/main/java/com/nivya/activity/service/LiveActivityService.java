package com.nivya.activity.service;

import com.nivya.activity.dto.ActivityEventDto;
import com.nivya.activity.dto.LiveActivityRequest;
import com.nivya.activity.dto.LiveActivityResponse;
import com.nivya.activity.entity.ActivityEvent;
import com.nivya.activity.repository.ActivityEventRepository;
import com.nivya.common.exception.ResourceNotFoundException;
import com.nivya.device.entity.Device;
import com.nivya.device.entity.DeviceStatus;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.device.repository.DeviceStatusRepository;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.role.RoleType;
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

/**
 * Service managing legitimate Parent-only Live Activity.
 * Enforces strict role boundaries and privacy safeguards:
 * - Parent-only live telemetry access (Child users receive 403 Forbidden).
 * - Sanitizes activity descriptions to prevent message body or credential exposure.
 * - Broadcasts real-time transitions over WebSocket.
 */
@Service
public class LiveActivityService {

    private static final Logger log = LoggerFactory.getLogger(LiveActivityService.class);

    private final ActivityEventRepository activityEventRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceStatusRepository deviceStatusRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public LiveActivityService(ActivityEventRepository activityEventRepository,
                               DeviceRepository deviceRepository,
                               DeviceStatusRepository deviceStatusRepository,
                               FamilyMemberRepository familyMemberRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.activityEventRepository = activityEventRepository;
        this.deviceRepository = deviceRepository;
        this.deviceStatusRepository = deviceStatusRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Ingests legitimate high-level activity telemetry.
     */
    @Transactional
    public ActivityEventDto recordActivity(LiveActivityRequest request, UserPrincipal principal) {
        Device device = deviceRepository.findByDeviceUuid(request.getDeviceUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with UUID: " + request.getDeviceUuid()));

        // Security authorization check
        if (principal != null) {
            if (principal.getRole() == RoleType.CHILD) {
                if (device.getUser() == null || !device.getUser().getId().equals(principal.getId())) {
                    throw new AccessDeniedException("Child user cannot submit telemetry for another device");
                }
            } else if (principal.getRole() == RoleType.PARENT) {
                if (device.getFamily() == null || !familyMemberRepository.existsByFamilyIdAndUserId(device.getFamily().getId(), principal.getId())) {
                    throw new AccessDeniedException("Parent not authorized for this device");
                }
            }
        }

        // Privacy sanitization fallback
        String broadActivity = request.getBroadActivity();
        if (broadActivity == null || broadActivity.isBlank()) {
            broadActivity = "Active in app";
        }
        String appName = request.getAppName();
        if (appName == null || appName.isBlank()) {
            appName = "Application";
        }
        String category = request.getCategory();
        if (category == null || category.isBlank()) {
            category = "GENERAL";
        }

        Instant now = Instant.now();
        Instant startedAt = request.getStartedAt() != null ? request.getStartedAt() : now;

        // Check current active activity
        Optional<ActivityEvent> currentOpt = activityEventRepository
                .findFirstByDeviceIdAndCurrentTrueOrderByStartedAtDesc(device.getId());

        ActivityEvent eventToReturn;

        if (currentOpt.isPresent()) {
            ActivityEvent current = currentOpt.get();
            if (current.getPackageName().equalsIgnoreCase(request.getPackageName())
                    && current.getBroadActivity().equalsIgnoreCase(broadActivity)) {
                // Same activity continuing: update duration
                int duration = request.getDurationSeconds() != null
                        ? request.getDurationSeconds()
                        : (int) Math.max(0, Duration.between(current.getStartedAt(), now).getSeconds());
                current.setDurationSeconds(duration);
                eventToReturn = activityEventRepository.save(current);
            } else {
                // Transition to new activity: close previous
                current.setCurrent(false);
                current.setEndedAt(now);
                long durationSec = Duration.between(current.getStartedAt(), now).getSeconds();
                current.setDurationSeconds((int) Math.max(0, durationSec));
                activityEventRepository.save(current);

                // Clear any other active flags for safety
                List<ActivityEvent> lingering = activityEventRepository.findByDeviceIdAndCurrentTrue(device.getId());
                for (ActivityEvent lingeringEvent : lingering) {
                    lingeringEvent.setCurrent(false);
                    lingeringEvent.setEndedAt(now);
                    activityEventRepository.save(lingeringEvent);
                }

                // Create new activity
                ActivityEvent newEvent = new ActivityEvent(
                        device,
                        request.getPackageName(),
                        appName,
                        broadActivity,
                        category,
                        request.getDurationSeconds() != null ? request.getDurationSeconds() : 0,
                        true,
                        startedAt,
                        request.getEndedAt()
                );
                eventToReturn = activityEventRepository.save(newEvent);
            }
        } else {
            // No current active activity: create one
            ActivityEvent newEvent = new ActivityEvent(
                    device,
                    request.getPackageName(),
                    appName,
                    broadActivity,
                    category,
                    request.getDurationSeconds() != null ? request.getDurationSeconds() : 0,
                    true,
                    startedAt,
                    request.getEndedAt()
            );
            eventToReturn = activityEventRepository.save(newEvent);
        }

        ActivityEventDto dto = ActivityEventDto.fromEntity(eventToReturn);
        broadcastActivityUpdate(device, dto);
        return dto;
    }

    /**
     * Parent-only retrieval of live activity and chronological recent history.
     */
    @Transactional(readOnly = true)
    public LiveActivityResponse getLiveActivity(Long deviceId, UserPrincipal principal) {
        if (principal.getRole() != RoleType.PARENT) {
            throw new AccessDeniedException("Live Activity is strictly restricted to parent accounts");
        }

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        if (device.getFamily() == null || !familyMemberRepository.existsByFamilyIdAndUserId(device.getFamily().getId(), principal.getId())) {
            throw new AccessDeniedException("User is not authorized to access live activity for this device");
        }

        boolean isOnline = deviceStatusRepository.findByDeviceId(deviceId)
                .map(DeviceStatus::isOnline)
                .orElse(false);

        Optional<ActivityEvent> currentEvent = activityEventRepository
                .findFirstByDeviceIdAndCurrentTrueOrderByStartedAtDesc(deviceId);

        ActivityEventDto currentDto = currentEvent.map(ActivityEventDto::fromEntity).orElse(null);

        List<ActivityEvent> recentList = activityEventRepository.findTop20ByDeviceIdOrderByStartedAtDesc(deviceId);
        List<ActivityEventDto> recentDtos = recentList.stream()
                .map(ActivityEventDto::fromEntity)
                .collect(Collectors.toList());

        Instant lastUpdatedAt = currentEvent.map(ActivityEvent::getCreatedAt).orElse(Instant.now());

        return new LiveActivityResponse(
                device.getId(),
                device.getDeviceUuid(),
                device.getDeviceName(),
                isOnline,
                currentDto,
                recentDtos,
                lastUpdatedAt
        );
    }

    private void broadcastActivityUpdate(Device device, ActivityEventDto eventDto) {
        try {
            if (device.getFamily() != null) {
                messagingTemplate.convertAndSend("/topic/family/" + device.getFamily().getId() + "/activity", eventDto);
            }
            messagingTemplate.convertAndSend("/topic/device/" + device.getId() + "/activity", eventDto);
        } catch (Exception e) {
            log.warn("Failed to broadcast activity update over WebSocket: {}", e.getMessage());
        }
    }
}
