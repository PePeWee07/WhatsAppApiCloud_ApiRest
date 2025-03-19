package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.requestVideo;

public record RequestMessage(
    String messaging_product,
    String recipient_type,
    String to,
    String type,
    RequestMessageVideoUrl video
) {
} 
