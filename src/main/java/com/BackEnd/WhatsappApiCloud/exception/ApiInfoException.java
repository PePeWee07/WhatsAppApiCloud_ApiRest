package com.BackEnd.WhatsappApiCloud.exception;

public class ApiInfoException extends RuntimeException {
    private final String infoMessage;
    private final String moderation;

    public ApiInfoException(String infoMessage, String moderation) {
        super(infoMessage);
        this.infoMessage = infoMessage;
        this.moderation = moderation; // Puede ser null
    }

    public String getInfoMessage() {
        return infoMessage;
    }

    public String getModeration() {
        return moderation;
    }
}


