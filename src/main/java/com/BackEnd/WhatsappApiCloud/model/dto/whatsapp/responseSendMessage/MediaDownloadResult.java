package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage;

public record MediaDownloadResult(
        byte[] bytes,
        String mimeType) {
}
