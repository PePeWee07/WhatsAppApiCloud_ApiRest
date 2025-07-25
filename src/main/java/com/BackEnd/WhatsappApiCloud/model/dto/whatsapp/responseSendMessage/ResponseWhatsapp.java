package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// * Respuesta al enviar un mensaje

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResponseWhatsapp(
        String messaging_product,
        List<ResponseWhatsappContact> contacts,
        List<ResponseWhatsappMessage> messages
) {
}
