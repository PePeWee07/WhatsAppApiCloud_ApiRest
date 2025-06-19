package com.BackEnd.WhatsappApiCloud.exception;

/**
 * Excepción que indica un fallo al manejar sesiones de chat en la BD.
 */
public class ChatSessionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ChatSessionException(String message) {
        super(message);
    }

    public ChatSessionException(String message, Throwable cause) {
        super(message, cause);
    }
}
