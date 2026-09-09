package com.nivya.convocation.dto;

import jakarta.validation.constraints.NotBlank;

public class ParentSendMessageRequest {

    private Long receiverUserId;

    @NotBlank(message = "Message text cannot be blank")
    private String message;

    public ParentSendMessageRequest() {
    }

    public ParentSendMessageRequest(Long receiverUserId, String message) {
        this.receiverUserId = receiverUserId;
        this.message = message;
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
