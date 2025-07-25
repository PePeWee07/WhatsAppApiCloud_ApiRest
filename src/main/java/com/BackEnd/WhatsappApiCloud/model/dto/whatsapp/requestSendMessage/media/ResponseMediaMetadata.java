package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.media;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResponseMediaMetadata (
    String url,
    @JsonProperty("mime_type") String mimeType,
    String sha256,
    @JsonProperty("file_size") Long fileSize,
    String id,
    @JsonProperty("messaging_product") String messagingProduct
) {}
