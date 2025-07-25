package com.BackEnd.WhatsappApiCloud.exception;

public class MediaNotFoundException extends RuntimeException {
    public MediaNotFoundException(String msg, Throwable cause) {
        super(msg, cause);
    }
}