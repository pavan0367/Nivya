package com.nivya.history.service;

import com.nivya.common.exception.ResourceNotFoundException;
import com.nivya.device.entity.Device;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.history.dto.HistoryEventDetailDto;
import com.nivya.history.dto.HistoryEventDto;
import com.nivya.history.dto.HistoryPageResponse;
import com.nivya.history.dto.RecordHistoryRequest;
import com.nivya.history.entity.HistoryEvent;
import com.nivya.history.repository.HistoryEventRepository;
import com.nivya.role.RoleType;
import com.nivya.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing chronological device activity history for Parent accounts.
 * Enforces strict role checks, pagination limits (max 100 items), and date/app filters.
 */
@Service
public class HistoryService {

    private static final Logger log = LoggerFactory.getLogger(HistoryService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final HistoryEventRepository historyEventRepository;
    private final DeviceRepository deviceRepository;
    private final FamilyMemberRepository familyMemberRepository;

    public HistoryService(HistoryEventRepository historyEventRepository,
                          DeviceRepository deviceRepository,
                          FamilyMemberRepository familyMemberRepository) {
        this.historyEventRepository = historyEventRepository;
        this.deviceRepository = deviceRepository;
        this.familyMemberRepository = familyMemberRepository;
    }

    /**
     * Parent-only: Retrieves filtered, paginated chronological history events.
     */
    @Transactional(readOnly = true)
    public HistoryPageResponse getHistory(Long deviceId, int page, int size,
                                          Instant startDate, Instant endDate, String application,
                                          UserPrincipal principal) {
        validateParentAccess(deviceId, principal);

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        Pageable pageable = PageRequest.of(safePage, safeSize);

        String appFilter = (application != null && !application.isBlank()) ? application.trim() : null;

        Page<HistoryEvent> eventsPage = historyEventRepository.findHistoryEvents(
                deviceId,
                startDate,
                endDate,
                appFilter,
                pageable
        );

        List<HistoryEventDto> dtos = eventsPage.getContent().stream()
                .map(HistoryEventDto::fromEntity)
                .collect(Collectors.toList());

        return new HistoryPageResponse(
                dtos,
                eventsPage.getNumber(),
                eventsPage.getTotalPages(),
                eventsPage.getTotalElements(),
                eventsPage.getSize(),
                eventsPage.hasNext(),
                eventsPage.hasPrevious()
        );
    }

    /**
     * Parent-only: Retrieves detailed event metadata within consented scope.
     */
    @Transactional(readOnly = true)
    public HistoryEventDetailDto getEventDetail(Long deviceId, Long eventId, UserPrincipal principal) {
        validateParentAccess(deviceId, principal);

        HistoryEvent event = historyEventRepository.findByIdAndDeviceId(eventId, deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("History event not found with ID: " + eventId));

        return HistoryEventDetailDto.fromEntity(event);
    }

    /**
     * Parent-only: Retrieves unique application names recorded for the device.
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctApplications(Long deviceId, UserPrincipal principal) {
        validateParentAccess(deviceId, principal);
        return historyEventRepository.findDistinctAppNamesByDeviceId(deviceId);
    }

    /**
     * Records legitimate historical activity event from device sync.
     */
    @Transactional
    public HistoryEventDto recordHistoryEvent(RecordHistoryRequest request, UserPrincipal principal) {
        Device device = deviceRepository.findByDeviceUuid(request.getDeviceUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with UUID: " + request.getDeviceUuid()));

        if (principal != null) {
            if (principal.getRole() == RoleType.CHILD) {
                if (device.getUser() == null || !device.getUser().getId().equals(principal.getId())) {
                    throw new AccessDeniedException("Child user not authorized to record history for another device");
                }
            } else if (principal.getRole() == RoleType.PARENT) {
                if (device.getFamily() == null || !familyMemberRepository.existsByFamilyIdAndUserId(device.getFamily().getId(), principal.getId())) {
                    throw new AccessDeniedException("Parent not authorized for this device");
                }
            }
        }

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

        HistoryEvent event = new HistoryEvent(
                device,
                request.getPackageName(),
                appName,
                broadActivity,
                request.getActivityLabel(),
                category,
                request.getDurationSeconds(),
                request.getEventTimestamp() != null ? request.getEventTimestamp() : Instant.now(),
                request.getDetails()
        );

        event = historyEventRepository.save(event);
        log.debug("Recorded history event id={} for device={}", event.getId(), device.getId());
        return HistoryEventDto.fromEntity(event);
    }

    private void validateParentAccess(Long deviceId, UserPrincipal principal) {
        if (principal.getRole() != RoleType.PARENT) {
            throw new AccessDeniedException("History is strictly restricted to parent accounts");
        }

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        if (device.getFamily() == null || !familyMemberRepository.existsByFamilyIdAndUserId(device.getFamily().getId(), principal.getId())) {
            throw new AccessDeniedException("User is not authorized to access history for this device");
        }
    }
}
