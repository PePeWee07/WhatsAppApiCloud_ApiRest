package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.media;

// * Usado por --> RequestMessage.java

public record RequestMessageText(
        boolean preview_url,
        String body
) {
}