package com.nivya.email.dto;

public class EmailSendResult {

    private final boolean success;
    private final String provider;
    private final String messageId;
    private final String errorMessage;

    public EmailSendResult(boolean success, String provider, String messageId, String errorMessage) {
        this.success = success;
        this.provider = provider;
        this.messageId = messageId;
        this.errorMessage = errorMessage;
    }

    public static EmailSendResult success(String provider, String messageId) {
        return new EmailSendResult(true, provider, messageId, null);
    }

    public static EmailSendResult failure(String provider, String errorMessage) {
        return new EmailSendResult(false, provider, null, errorMessage);
    }

    public boolean isSuccess() { return success; }
    public String getProvider() { return provider; }
    public String getMessageId() { return messageId; }
    public String getErrorMessage() { return errorMessage; }
}
