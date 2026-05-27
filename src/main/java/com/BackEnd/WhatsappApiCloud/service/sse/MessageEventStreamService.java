package com.BackEnd.WhatsappApiCloud.service.sse;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MessageEventStreamService {

    private static final Logger logger = LoggerFactory.getLogger(MessageEventStreamService.class);

    private final ObjectMapper objectMapper;

    // Emitters por conversación (phone)
    private final Map<String, Set<SseEmitter>> emittersByPhone = new ConcurrentHashMap<>();

    public MessageEventStreamService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Registra un cliente SSE para un phone.
     */
    public SseEmitter subscribe(String phone) {
        // 0 = sin timeout (conexión larga).
        SseEmitter emitter = new SseEmitter(0L);

        emittersByPhone.computeIfAbsent(phone, k -> ConcurrentHashMap.newKeySet()).add(emitter);

        // Limpieza automática
        emitter.onCompletion(() -> {
            logger.info("SSE completed for phone: {}", phone);
            remove(phone, emitter);
        });
        emitter.onTimeout(() -> {
            logger.info("SSE TimeOut for phone: {}", phone);
            remove(phone, emitter);
        });
        emitter.onError((ex) -> {
            logger.info("SSE Error for phone: {}", phone);
            remove(phone, emitter);
        });

        // (opcional) para confirmar conexión
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("ok", true);
            payload.put("phone", phone);

            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(objectMapper.writeValueAsString(payload))
                    .id(String.valueOf(Instant.now().toEpochMilli())));
        } catch (JsonProcessingException ex) {
            logger.warn("No se pudo serializar evento SSE connected para phone {}", phone, ex);
        } catch (IOException ignored) {
        }

        return emitter;
    }

    private void remove(String phone, SseEmitter emitter) {
        Set<SseEmitter> set = emittersByPhone.get(phone);
        if (set != null) {
            set.remove(emitter);
            if (set.isEmpty()) {
                emittersByPhone.remove(phone);
            }
        }
    }

    public boolean hasSubscribers(String phone) {
        Set<SseEmitter> set = emittersByPhone.get(phone);
        return set != null && !set.isEmpty();
    }

    /**
     * Notifica a los clientes SSE que hay novedades para ese phone.
     */
    public void notifyUpdate(String phone, String eventType, MessageDto message) {
        Set<SseEmitter> set = emittersByPhone.get(phone);
        if (set == null || set.isEmpty()) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("phone", phone);
        payload.put("message", message);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            logger.warn("No se pudo serializar payload SSE para phone {}", phone, ex);
            return;
        }

        for (SseEmitter emitter : set) {
            try {
                emitter.send(SseEmitter.event()
                        .name("message_update")
                        .data(payloadJson));
            } catch (Exception ex) {
                remove(phone, emitter);
            }
        }
    }

    public void notifyStatusUpdate(String phone, String eventType, String status, MessageDto message) {
        Set<SseEmitter> set = emittersByPhone.get(phone);
        if (set == null || set.isEmpty()) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("phone", phone);
        payload.put("status", status);
        payload.put("message", message);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            logger.warn("No se pudo serializar payload SSE de estado para phone {}", phone, ex);
            return;
        }

        for (SseEmitter emitter : set) {
            try {
                emitter.send(SseEmitter.event()
                        .name("message_status_update")
                        .data(payloadJson));
            } catch (Exception ex) {
                remove(phone, emitter);
            }
        }
    }

}
