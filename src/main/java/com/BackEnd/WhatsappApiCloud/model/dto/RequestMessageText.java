package com.BackEnd.WhatsappApiCloud.model.dto;

// * Usado por --> RequestMessage.java

public record RequestMessageText(
        boolean preview_url,
        String body
) {
}