package com.BackEnd.WhatsappApiCloud.service.sse;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.BackEnd.WhatsappApiCloud.util.enums.MessageTypeEnum;

@Service
public class MessageEventStreamService {

    // Emitters por conversación (phone)
    private final Map<String, Set<SseEmitter>> emittersByPhone = new ConcurrentHashMap<>();

    /**
     * Registra un cliente SSE para un phone.
    */
    public SseEmitter subscribe(String phone) {
        // 0 = sin timeout (conexión larga).
        SseEmitter emitter = new SseEmitter(0L);

        emittersByPhone.computeIfAbsent(phone, k -> ConcurrentHashMap.newKeySet()).add(emitter);

        // Limpieza automática
        emitter.onCompletion(() -> remove(phone, emitter));
        emitter.onTimeout(() -> remove(phone, emitter));
        emitter.onError((ex) -> remove(phone, emitter));

        // (opcional) para confirmar conexión
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"ok\":true,\"phone\":\"" + phone + "\"}")
                    .id(String.valueOf(Instant.now().toEpochMilli())));
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

    /**
     * Notifica a los clientes SSE que hay novedades para ese phone.
    */
    public void notifyUpdate(String phone, String eventType, MessageTypeEnum messageType, String textBody) {
    Set<SseEmitter> set = emittersByPhone.get(phone);
    if (set == null || set.isEmpty()) return;

    int max = 140;
    String preview = textBody == null ? "" : textBody;
    preview = preview.replace("\r", " ").replace("\n", " ").trim();

    if (preview.length() > max) {
        preview = preview.substring(0, max) + "...";
    }

    // escape básico para JSON
    String safePreview = preview
        .replace("\\", "\\\\")
        .replace("\"", "\\\"");

    String payload = String.format(
        "{\"eventType\":\"%s\",\"phone\":\"%s\",\"messageType\":\"%s\",\"preview\":\"%s\"}",
        eventType,
        phone,
        messageType == null ? "" : messageType.name(),
        safePreview
    );

    for (SseEmitter emitter : set) {
        try {
            emitter.send(SseEmitter.event()
                .name("message_update")
                .data(payload));
        } catch (Exception ex) {
            remove(phone, emitter);
        }
    }
}

}
