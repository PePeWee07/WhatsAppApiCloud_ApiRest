package com.BackEnd.WhatsappApiCloud.exception;

public class CustomJsonServerException extends RuntimeException {
    public CustomJsonServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
