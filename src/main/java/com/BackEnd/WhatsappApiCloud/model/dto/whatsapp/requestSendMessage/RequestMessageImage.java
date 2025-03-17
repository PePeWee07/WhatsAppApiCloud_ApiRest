package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage;

public record RequestMessageImage(
    String messaging_product,
    String recipient_type,
    String to,
    String type,
    RequestMessageText text
) {
} 
