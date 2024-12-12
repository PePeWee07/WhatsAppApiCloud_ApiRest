package com.BackEnd.WhatsappApiCloud.exception;

public class ApiInfoException extends RuntimeException {
    private final String infoMessage;

    public ApiInfoException(String infoMessage) {
        super(infoMessage);
        this.infoMessage = infoMessage;
    }

    public String getInfoMessage() {
        return infoMessage;
    }
}

