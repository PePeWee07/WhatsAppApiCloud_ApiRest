package com.BackEnd.WhatsappApiCloud.exception;

import java.util.Map;

import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeParseException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUsuarioNotFound(UserNotFoundException ex) {
        Map<String, String> body = Map.of("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<Map<String, String>> handleNumberFormat(NumberFormatException ex) {
        Map<String, String> body = Map.of("error", "Valor numérico inválido: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ErpNotFoundException.class)
    public ResponseEntity<Map<String,String>> handleErpNotFound(ErpNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ServerClientException.class)
    public ResponseEntity<Map<String, String>> handleErpError(ServerClientException ex) {
        Map<String, String> body = Map.of("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler({ DateTimeParseException.class })
    public ResponseEntity<Map<String, String>> handleDateTimeParse(DateTimeParseException ex) {
        Map<String, String> body = Map.of("error", "Formato de fecha inválido. Use 'YYYY-MM-DDThh:mm:ss'.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<Map<String, String>> handlePropertyReference(PropertyReferenceException ex) {
        Map<String, String> body = Map.of("error", "Campo de orden inválido: " + ex.getPropertyName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> body = Map.of("error", "Parámetro inválido: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage()));
    }


    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxSize(MaxUploadSizeExceededException ex) {
        Map<String, String> body = Map.of("error", "El archivo supera el tamaño máximo permitido.");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    @ExceptionHandler({
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class,
            MultipartException.class
    })
    public ResponseEntity<Map<String, String>> handleBadRequestParameters(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Parámetro incorrecto."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(Map.of("error", "Tipo de archivo no permitido."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(Map.of("error", ex.getReason()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String errorMessage = "Error en el formato del JSON enviado. Verifica los datos.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", errorMessage));
    }

    @ExceptionHandler(GlpiNotFoundException.class)
    public ResponseEntity<Map<String,String>> handleGlpiNotFound(GlpiNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    // Manejador genérico para cualquier otra excepción no contemplada
    // @ExceptionHandler(Exception.class)
    // public ResponseEntity<Map<String,String>> handleAnyException(Exception ex) {
    // Map<String,String> body = Map.of("error", "Ocurrió un error inesperado.
    // Intenta nuevamente. " + ex);
    // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    // }
}
