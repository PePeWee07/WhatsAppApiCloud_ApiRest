package com.BackEnd.WhatsappApiCloud.exception;

/** 
 * Se lanza cuando GLPI responde 404 o no encuentra el recurso pedido.
 */
public class GlpiNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public GlpiNotFoundException(String message) {
        super(message);
    }

    public GlpiNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
