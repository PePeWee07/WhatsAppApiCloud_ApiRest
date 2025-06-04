package com.BackEnd.WhatsappApiCloud.exception;

public class ServerClientException extends RuntimeException {
    public ServerClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
