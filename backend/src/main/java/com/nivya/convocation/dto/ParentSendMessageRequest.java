package com.nivya.convocation.dto;

import jakarta.validation.constraints.NotBlank;

public class ParentSendMessageRequest {

    private Long receiverUserId;
    private Long replyToId;
    private Long targetDeviceId;

    @NotBlank(message = "Message text cannot be blank")
    private String message;

    public ParentSendMessageRequest() {
    }

    public ParentSendMessageRequest(Long receiverUserId, String message) {
        this.receiverUserId = receiverUserId;
        this.message = message;
    }

    public ParentSendMessageRequest(Long receiverUserId, String message, Long replyToId) {
        this.receiverUserId = receiverUserId;
        this.message = message;
        this.replyToId = replyToId;
    }

    public ParentSendMessageRequest(Long receiverUserId, String message, Long replyToId, Long targetDeviceId) {
        this.receiverUserId = receiverUserId;
        this.message = message;
        this.replyToId = replyToId;
        this.targetDeviceId = targetDeviceId;
    }

    public Long getTargetDeviceId() {
        return targetDeviceId;
    }

    public void setTargetDeviceId(Long targetDeviceId) {
        this.targetDeviceId = targetDeviceId;
    }

    public Long getReplyToId() {
        return replyToId;
    }

    public void setReplyToId(Long replyToId) {
        this.replyToId = replyToId;
    }

    public Long getReceiverUserId() {
        return receiverUserId;
    }

    public void setReceiverUserId(Long receiverUserId) {
        this.receiverUserId = receiverUserId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
