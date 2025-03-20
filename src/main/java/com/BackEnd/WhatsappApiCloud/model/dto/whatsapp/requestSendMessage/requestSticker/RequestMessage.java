package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.requestSticker;

public record RequestMessage(
    String messaging_product,
    String recipient_type,
    String to,
    String type,
    RequestMessageStickerUrl sticker
) {
} 
