package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

//* Indica al usuario que estoy escribiendo
@JsonIgnoreProperties(ignoreUnknown = true)
public record TypingIndicator(
    String type
) {}
