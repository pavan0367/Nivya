package com.nivya.convocation.controller;

import com.nivya.common.response.ApiResponse;
import com.nivya.convocation.dto.*;
import com.nivya.convocation.service.ConvocationService;
import com.nivya.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/convocation")
@Tag(name = "Convocation", description = "Priority family convocation messaging and ephemeral viewing")
@SecurityRequirement(name = "Bearer Authentication")
public class ConvocationController {

    private final ConvocationService convocationService;

    public ConvocationController(ConvocationService convocationService) {
        this.convocationService = convocationService;
    }

    // =========================================================================
    // PARENT ENDPOINTS
    // =========================================================================

    @PostMapping("/parent/send")
    @Operation(summary = "Send Convocation message (Parent only)", description = "Sends a priority guidance message to Child device")
    public ResponseEntity<ApiResponse<ParentConvocationMessageDto>> parentSendMessage(
            @Valid @RequestBody ParentSendMessageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ParentConvocationMessageDto dto = convocationService.parentSendMessage(principal, request);
        return ResponseEntity.ok(ApiResponse.success(dto, "Convocation message sent successfully"));
    }

    @GetMapping("/parent/history")
    @Operation(summary = "Get retained history (Parent only)", description = "Retrieves complete, permanent retained history for family")
    public ResponseEntity<ApiResponse<List<ParentConvocationMessageDto>>> parentGetRetainedHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ParentConvocationMessageDto> history = convocationService.parentGetRetainedHistory(principal);
        return ResponseEntity.ok(ApiResponse.success(history, "Retained history retrieved successfully"));
    }

    @GetMapping("/parent/seen")
    @Operation(summary = "Get Seen status map (Parent only)", description = "Retrieves Seen state map of sent messages")
    public ResponseEntity<ApiResponse<Map<Long, Boolean>>> parentGetSeenState(
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<Long, Boolean> seenMap = convocationService.parentGetSeenState(principal);
        return ResponseEntity.ok(ApiResponse.success(seenMap, "Seen state retrieved successfully"));
    }

    // =========================================================================
    // CHILD ENDPOINTS
    // =========================================================================

    @GetMapping("/child/unread")
    @Operation(summary = "Get unread messages (Child only)", description = "Retrieves currently unread messages without revealing Seen state")
    public ResponseEntity<ApiResponse<List<ChildConvocationMessageDto>>> childGetUnreadMessages(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ChildConvocationMessageDto> unread = convocationService.childGetUnreadMessages(principal);
        return ResponseEntity.ok(ApiResponse.success(unread, "Unread messages retrieved"));
    }

    @PostMapping("/child/view/start")
    @Operation(summary = "Start 2-minute viewing session (Child only)", description = "Activates viewing for all currently unread messages with 2-minute server expiration")
    public ResponseEntity<ApiResponse<ChildViewingSessionResponse>> childStartViewing(
            @AuthenticationPrincipal UserPrincipal principal) {
        ChildViewingSessionResponse session = convocationService.childStartViewing(principal);
        return ResponseEntity.ok(ApiResponse.success(session, "Viewing session activated"));
    }

    @PostMapping("/child/send")
    @Operation(summary = "Send note to parent (Child only)", description = "Sends message that disappears from child view and is retained by parent")
    public ResponseEntity<ApiResponse<Map<String, Object>>> childSendMessage(
            @Valid @RequestBody ChildSendMessageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> ack = convocationService.childSendMessage(principal, request);
        return ResponseEntity.ok(ApiResponse.success(ack, "Note delivered to family"));
    }

    @GetMapping("/child/visibility")
    @Operation(summary = "Get visibility state (Child only)", description = "Retrieves current viewing session status and countdown")
    public ResponseEntity<ApiResponse<ChildVisibilityStateResponse>> childGetVisibilityState(
            @AuthenticationPrincipal UserPrincipal principal) {
        ChildVisibilityStateResponse state = convocationService.childGetVisibilityState(principal);
        return ResponseEntity.ok(ApiResponse.success(state, "Visibility state retrieved"));
    }
}
