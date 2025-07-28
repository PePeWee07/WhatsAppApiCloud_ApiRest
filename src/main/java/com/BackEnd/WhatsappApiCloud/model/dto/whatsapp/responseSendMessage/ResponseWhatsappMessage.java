package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

//* Usado por --> ResponseWhatsapp.java
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResponseWhatsappMessage(
    String id,
    @JsonProperty("message_status")
    String messageStatus
) {
    public ResponseWhatsappMessage(String id) {
        this(id, null);
    }
}
