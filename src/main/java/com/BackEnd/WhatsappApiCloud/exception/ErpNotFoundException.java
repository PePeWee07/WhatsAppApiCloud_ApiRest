package com.BackEnd.WhatsappApiCloud.exception;

/**
 * Excepción que indica que no se encontró el usuario en el servicio ERP.
 */
public class ErpNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ErpNotFoundException(String message) {
        super(message);
    }

    public ErpNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
