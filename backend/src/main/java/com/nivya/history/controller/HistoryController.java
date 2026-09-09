package com.nivya.history.controller;

import com.nivya.common.response.ApiResponse;
import com.nivya.history.dto.HistoryEventDetailDto;
import com.nivya.history.dto.HistoryEventDto;
import com.nivya.history.dto.HistoryPageResponse;
import com.nivya.history.dto.RecordHistoryRequest;
import com.nivya.history.service.HistoryService;
import com.nivya.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/history")
@Tag(name = "Parent History", description = "Parent-only chronological activity timeline and event inspection within consented scope")
@SecurityRequirement(name = "Bearer Authentication")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "Get paginated history (Parent only)", description = "Retrieves chronological activity timeline with date and application filters")
    public ResponseEntity<ApiResponse<HistoryPageResponse>> getHistory(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String application,
            @AuthenticationPrincipal UserPrincipal principal) {

        HistoryPageResponse response = historyService.getHistory(
                deviceId, page, size, startDate, endDate, application, principal
        );
        return ResponseEntity.ok(ApiResponse.success(response, "History retrieved successfully"));
    }

    @GetMapping("/{deviceId}/events/{eventId}")
    @Operation(summary = "Get event detail (Parent only)", description = "Retrieves specific activity event details within consented scope")
    public ResponseEntity<ApiResponse<HistoryEventDetailDto>> getEventDetail(
            @PathVariable Long deviceId,
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {

        HistoryEventDetailDto detail = historyService.getEventDetail(deviceId, eventId, principal);
        return ResponseEntity.ok(ApiResponse.success(detail, "Event detail retrieved successfully"));
    }

    @GetMapping("/{deviceId}/applications")
    @Operation(summary = "Get distinct applications (Parent only)", description = "Returns unique application names recorded for device filtering")
    public ResponseEntity<ApiResponse<List<String>>> getDistinctApplications(
            @PathVariable Long deviceId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<String> apps = historyService.getDistinctApplications(deviceId, principal);
        return ResponseEntity.ok(ApiResponse.success(apps, "Distinct applications retrieved successfully"));
    }

    @PostMapping("/events")
    @Operation(summary = "Record history event", description = "Records historical activity event from device sync worker")
    public ResponseEntity<ApiResponse<HistoryEventDto>> recordEvent(
            @Valid @RequestBody RecordHistoryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        HistoryEventDto dto = historyService.recordHistoryEvent(request, principal);
        return ResponseEntity.ok(ApiResponse.success(dto, "History event recorded successfully"));
    }
}
