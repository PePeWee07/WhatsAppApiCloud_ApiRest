package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage;

//* Respuesta para marcar un mensaje como leído
public record RequestWhatsappAsRead(
    String messaging_product,
    String status,
    String message_id
) {
}
