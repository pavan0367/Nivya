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
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Send Convocation message (Parent only)", description = "Sends a priority guidance message to Child device")
    public ResponseEntity<ApiResponse<ParentConvocationMessageDto>> parentSendMessage(
            @Valid @RequestBody ParentSendMessageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ParentConvocationMessageDto dto = convocationService.parentSendMessage(principal, request);
        return ResponseEntity.ok(ApiResponse.success(dto, "Convocation message sent successfully"));
    }

    @GetMapping("/parent/history")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Get retained history (Parent only)", description = "Retrieves complete, permanent retained history for family")
    public ResponseEntity<ApiResponse<List<ParentConvocationMessageDto>>> parentGetRetainedHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ParentConvocationMessageDto> history = convocationService.parentGetRetainedHistory(principal);
        return ResponseEntity.ok(ApiResponse.success(history, "Retained history retrieved successfully"));
    }

    @GetMapping("/parent/seen")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Get Seen status map (Parent only)", description = "Retrieves Seen state map of sent messages")
    public ResponseEntity<ApiResponse<Map<Long, Boolean>>> parentGetSeenState(
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<Long, Boolean> seenMap = convocationService.parentGetSeenState(principal);
        return ResponseEntity.ok(ApiResponse.success(seenMap, "Seen state retrieved successfully"));
    }

    @PostMapping("/parent/message/{messageId}/unsend")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Unsend message (Parent only)", description = "Unsends a Parent-owned message and removes it from visible conversation")
    public ResponseEntity<ApiResponse<Void>> parentUnsendMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserPrincipal principal) {
        convocationService.parentUnsendMessage(principal, messageId);
        return ResponseEntity.ok(ApiResponse.success(null, "Message unsent successfully"));
    }

    @PostMapping("/parent/message/{messageId}/pin")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Toggle pin message (Parent only)", description = "Toggles pin status of a message")
    public ResponseEntity<ApiResponse<ParentConvocationMessageDto>> parentTogglePinMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ParentConvocationMessageDto dto = convocationService.parentTogglePinMessage(principal, messageId);
        return ResponseEntity.ok(ApiResponse.success(dto, "Message pin status updated"));
    }

    @PostMapping("/parent/message/{messageId}/react")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "React to message (Parent only)", description = "Adds or updates reaction on a message")
    public ResponseEntity<ApiResponse<ParentConvocationMessageDto>> parentReactToMessage(
            @PathVariable Long messageId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String reaction = body != null ? body.get("reaction") : null;
        ParentConvocationMessageDto dto = convocationService.parentReactToMessage(principal, messageId, reaction);
        return ResponseEntity.ok(ApiResponse.success(dto, "Reaction updated"));
    }

    // =========================================================================
    // CHILD ENDPOINTS
    // =========================================================================

    @GetMapping("/child/unread")
    @PreAuthorize("hasRole('CHILD')")
    @Operation(summary = "Get unread messages (Child only)", description = "Retrieves currently unread messages without revealing Seen state")
    public ResponseEntity<ApiResponse<List<ChildConvocationMessageDto>>> childGetUnreadMessages(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ChildConvocationMessageDto> unread = convocationService.childGetUnreadMessages(principal);
        return ResponseEntity.ok(ApiResponse.success(unread, "Unread messages retrieved"));
    }

    @PostMapping("/child/view/start")
    @PreAuthorize("hasRole('CHILD')")
    @Operation(summary = "Start 2-minute viewing session (Child only)", description = "Activates viewing for all currently unread messages with 2-minute server expiration")
    public ResponseEntity<ApiResponse<ChildViewingSessionResponse>> childStartViewing(
            @AuthenticationPrincipal UserPrincipal principal) {
        ChildViewingSessionResponse session = convocationService.childStartViewing(principal);
        return ResponseEntity.ok(ApiResponse.success(session, "Viewing session activated"));
    }

    @PostMapping("/child/send")
    @PreAuthorize("hasRole('CHILD')")
    @Operation(summary = "Send note to parent (Child only)", description = "Sends message that disappears from child view and is retained by parent")
    public ResponseEntity<ApiResponse<Map<String, Object>>> childSendMessage(
            @Valid @RequestBody ChildSendMessageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> ack = convocationService.childSendMessage(principal, request);
        return ResponseEntity.ok(ApiResponse.success(ack, "Note delivered to family"));
    }

    @GetMapping("/child/visibility")
    @PreAuthorize("hasRole('CHILD')")
    @Operation(summary = "Get visibility state (Child only)", description = "Retrieves current viewing session status and countdown")
    public ResponseEntity<ApiResponse<ChildVisibilityStateResponse>> childGetVisibilityState(
            @AuthenticationPrincipal UserPrincipal principal) {
        ChildVisibilityStateResponse state = convocationService.childGetVisibilityState(principal);
        return ResponseEntity.ok(ApiResponse.success(state, "Visibility state retrieved"));
    }
}
