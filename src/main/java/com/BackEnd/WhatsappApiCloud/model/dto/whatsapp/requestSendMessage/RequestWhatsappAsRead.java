package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

//* Respuesta para marcar un mensaje como leído
@JsonIgnoreProperties(ignoreUnknown = true)
public record RequestWhatsappAsRead(
    @JsonProperty("messaging_product")
    String messagingProduct,
    String status,
    String message_id,
    @JsonProperty("typing_indicator")
    TypingIndicator typingIndicator // Cambiado de String a TypingIndicator
) {
    public RequestWhatsappAsRead(String messagingProduct, String status, String message_id) {
        this(messagingProduct, status, message_id, null);
    }
}
