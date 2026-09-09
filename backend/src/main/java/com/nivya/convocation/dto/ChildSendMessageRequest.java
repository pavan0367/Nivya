package com.nivya.convocation.dto;

import jakarta.validation.constraints.NotBlank;

public class ChildSendMessageRequest {

    @NotBlank(message = "Message text cannot be blank")
    private String message;

    private String clientMessageId;

    public ChildSendMessageRequest() {
    }

    public ChildSendMessageRequest(String message) {
        this.message = message;
    }

    public ChildSendMessageRequest(String message, String clientMessageId) {
        this.message = message;
        this.clientMessageId = clientMessageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getClientMessageId() {
        return clientMessageId;
    }

    public void setClientMessageId(String clientMessageId) {
        this.clientMessageId = clientMessageId;
    }
}
