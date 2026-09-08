package com.nivya.usage.service;

import com.nivya.common.exception.ResourceNotFoundException;
import com.nivya.device.entity.Device;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.security.UserPrincipal;
import com.nivya.usage.dto.AppUsageResponse;
import com.nivya.usage.dto.UsageSummaryResponse;
import com.nivya.usage.dto.UsageTelemetryRequest;
import com.nivya.usage.dto.UsageTrendResponse;
import com.nivya.usage.entity.UsageApp;
import com.nivya.usage.entity.UsageSummary;
import com.nivya.usage.repository.UsageAppRepository;
import com.nivya.usage.repository.UsageSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UsageService {

    private static final Logger log = LoggerFactory.getLogger(UsageService.class);

    private final UsageSummaryRepository usageSummaryRepository;
    private final UsageAppRepository usageAppRepository;
    private final DeviceRepository deviceRepository;
    private final FamilyMemberRepository familyMemberRepository;

    public UsageService(UsageSummaryRepository usageSummaryRepository,
                        UsageAppRepository usageAppRepository,
                        DeviceRepository deviceRepository,
                        FamilyMemberRepository familyMemberRepository) {
        this.usageSummaryRepository = usageSummaryRepository;
        this.usageAppRepository = usageAppRepository;
        this.deviceRepository = deviceRepository;
        this.familyMemberRepository = familyMemberRepository;
    }

    @Transactional
    public UsageSummaryResponse recordUsage(UsageTelemetryRequest request, UserPrincipal principal) {
        Device device = deviceRepository.findByDeviceUuid(request.getDeviceUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with UUID: " + request.getDeviceUuid()));

        validateDeviceAccess(device, principal);

        LocalDate date = request.getDate() != null ? request.getDate() : LocalDate.now();

        // 1. Calculate Category Sums from app items
        long eduSec = 0;
        long recSec = 0;
        long socSec = 0;
        long prodSec = 0;
        long computedTotalSec = 0;

        for (UsageTelemetryRequest.AppUsageItemDto item : request.getApps()) {
            long sec = item.getForegroundSeconds() != null ? item.getForegroundSeconds() : 0L;
            computedTotalSec += sec;
            String cat = item.getCategory() != null ? item.getCategory().toUpperCase() : "OTHER";
            switch (cat) {
                case "EDUCATION":
                case "LEARNING":
                    eduSec += sec;
                    break;
                case "GAMES":
                case "ENTERTAINMENT":
                case "RECREATION":
                    recSec += sec;
                    break;
                case "SOCIAL":
                case "COMMUNICATION":
                    socSec += sec;
                    break;
                case "PRODUCTIVITY":
                case "UTILITIES":
                    prodSec += sec;
                    break;
                default:
                    break;
            }
        }

        long totalSec = request.getTotalForegroundSeconds() != null && request.getTotalForegroundSeconds() > 0
                ? request.getTotalForegroundSeconds() : computedTotalSec;

        // 2. Upsert Daily UsageSummary
        UsageSummary summary = usageSummaryRepository.findByDeviceIdAndDate(device.getId(), date)
                .orElse(new UsageSummary(device, date, 0L, 0, 0L, 0L, 0L, 0L));

        summary.setTotalForegroundSeconds(totalSec);
        if (request.getScreenUnlocks() != null) {
            summary.setScreenUnlocks(request.getScreenUnlocks());
        }
        summary.setEducationalSeconds(eduSec);
        summary.setRecreationalSeconds(recSec);
        summary.setSocialSeconds(socSec);
        summary.setProductivitySeconds(prodSec);
        summary.setUpdatedAt(Instant.now());
        summary = usageSummaryRepository.save(summary);

        // 3. Upsert App Usage details
        for (UsageTelemetryRequest.AppUsageItemDto item : request.getApps()) {
            UsageApp app = usageAppRepository.findBySummaryIdAndPackageName(summary.getId(), item.getPackageName())
                    .orElse(new UsageApp(summary, item.getPackageName(), item.getAppName(), item.getCategory(), 0L, null));

            app.setAppName(item.getAppName());
            app.setCategory(item.getCategory() != null ? item.getCategory().toUpperCase() : "OTHER");
            app.setForegroundSeconds(item.getForegroundSeconds() != null ? item.getForegroundSeconds() : 0L);
            if (item.getLastTimeUsed() != null) {
                app.setLastTimeUsed(item.getLastTimeUsed());
            }
            usageAppRepository.save(app);
        }

        log.info("Recorded screen time for device {}: date={}, totalTime={}",
                device.getDeviceUuid(), date, formatDuration(totalSec));

        return toSummaryResponse(summary);
    }

    @Transactional(readOnly = true)
    public UsageSummaryResponse getDailySummary(Long deviceId, LocalDate date, UserPrincipal principal) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        validateDeviceAccess(device, principal);

        LocalDate targetDate = date != null ? date : LocalDate.now();
        UsageSummary summary = usageSummaryRepository.findByDeviceIdAndDate(deviceId, targetDate)
                .orElseGet(() -> new UsageSummary(device, targetDate, 0L, 0, 0L, 0L, 0L, 0L));

        return toSummaryResponse(summary);
    }

    @Transactional(readOnly = true)
    public AppUsageResponse getAppUsage(Long deviceId, LocalDate date, UserPrincipal principal) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        validateDeviceAccess(device, principal);

        LocalDate targetDate = date != null ? date : LocalDate.now();
        Optional<UsageSummary> summaryOpt = usageSummaryRepository.findByDeviceIdAndDate(deviceId, targetDate);

        if (summaryOpt.isEmpty()) {
            return new AppUsageResponse(deviceId, targetDate, Collections.emptyList());
        }

        UsageSummary summary = summaryOpt.get();
        List<UsageApp> apps = usageAppRepository.findBySummaryIdOrderByForegroundSecondsDesc(summary.getId());
        long totalSec = Math.max(summary.getTotalForegroundSeconds(), 1L);

        List<AppUsageResponse.AppUsageDetailDto> details = apps.stream()
                .map(app -> {
                    double pct = (double) app.getForegroundSeconds() / totalSec;
                    return new AppUsageResponse.AppUsageDetailDto(
                            app.getPackageName(),
                            app.getAppName(),
                            app.getCategory(),
                            app.getForegroundSeconds(),
                            formatDuration(app.getForegroundSeconds()),
                            Math.round(pct * 100.0) / 100.0,
                            app.getLastTimeUsed()
                    );
                })
                .collect(Collectors.toList());

        return new AppUsageResponse(deviceId, targetDate, details);
    }

    @Transactional(readOnly = true)
    public UsageTrendResponse getUsageTrends(Long deviceId, UserPrincipal principal) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        validateDeviceAccess(device, principal);

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        LocalDate prevWeekStart = today.minusDays(13);
        LocalDate prevWeekEnd = today.minusDays(7);

        List<UsageSummary> currentWeekSummaries = usageSummaryRepository
                .findByDeviceIdAndDateBetweenOrderByDateAsc(deviceId, weekStart, today);

        List<UsageSummary> prevWeekSummaries = usageSummaryRepository
                .findByDeviceIdAndDateBetweenOrderByDateAsc(deviceId, prevWeekStart, prevWeekEnd);

        long currentTotal = currentWeekSummaries.stream().mapToLong(UsageSummary::getTotalForegroundSeconds).sum();
        long prevTotal = prevWeekSummaries.stream().mapToLong(UsageSummary::getTotalForegroundSeconds).sum();

        double pctChange = 0.0;
        if (prevTotal > 0) {
            pctChange = Math.round(((double) (currentTotal - prevTotal) / prevTotal) * 1000.0) / 10.0;
        }

        String description = pctChange <= 0
                ? "Screen time is down " + Math.abs(pctChange) + "% compared to last week."
                : "Screen time increased by " + pctChange + "% compared to last week.";

        List<UsageTrendResponse.DailyPointDto> dailyPoints = currentWeekSummaries.stream()
                .map(s -> new UsageTrendResponse.DailyPointDto(
                        s.getDate(),
                        s.getTotalForegroundSeconds(),
                        formatDuration(s.getTotalForegroundSeconds()),
                        s.getEducationalSeconds()
                ))
                .collect(Collectors.toList());

        return new UsageTrendResponse(
                deviceId,
                dailyPoints,
                currentTotal,
                prevTotal,
                pctChange,
                description
        );
    }

    private void validateDeviceAccess(Device device, UserPrincipal principal) {
        if (principal == null) return;
        boolean isOwner = device.getUser().getId().equals(principal.getId());
        boolean isFamilyMember = device.getFamily() != null &&
                familyMemberRepository.findByFamilyIdAndUserId(device.getFamily().getId(), principal.getId()).isPresent();

        if (!isOwner && !isFamilyMember) {
            throw new AccessDeniedException("Unauthorized access to device usage statistics.");
        }
    }

    private String formatDuration(long totalSeconds) {
        if (totalSeconds < 60) {
            return totalSeconds + "s";
        }
        long minutes = totalSeconds / 60;
        if (minutes < 60) {
            return minutes + "m";
        }
        long hours = minutes / 60;
        long remMinutes = minutes % 60;
        return remMinutes > 0 ? hours + "h " + remMinutes + "m" : hours + "h";
    }

    private UsageSummaryResponse toSummaryResponse(UsageSummary summary) {
        Map<String, Long> categoryBreakdown = new HashMap<>();
        categoryBreakdown.put("EDUCATION", summary.getEducationalSeconds());
        categoryBreakdown.put("GAMES", summary.getRecreationalSeconds());
        categoryBreakdown.put("SOCIAL", summary.getSocialSeconds());
        categoryBreakdown.put("PRODUCTIVITY", summary.getProductivitySeconds());

        return new UsageSummaryResponse(
                summary.getDevice().getId(),
                summary.getDevice().getDeviceUuid(),
                summary.getDevice().getDeviceName(),
                summary.getDate(),
                summary.getTotalForegroundSeconds(),
                formatDuration(summary.getTotalForegroundSeconds()),
                summary.getEducationalSeconds(),
                summary.getRecreationalSeconds(),
                summary.getSocialSeconds(),
                summary.getProductivitySeconds(),
                summary.getScreenUnlocks(),
                categoryBreakdown
        );
    }
}
