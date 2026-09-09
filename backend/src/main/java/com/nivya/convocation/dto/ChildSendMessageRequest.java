package com.nivya.convocation.dto;

import jakarta.validation.constraints.NotBlank;

public class ChildSendMessageRequest {

    @NotBlank(message = "Message text cannot be blank")
    private String message;

    public ChildSendMessageRequest() {
    }

    public ChildSendMessageRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
