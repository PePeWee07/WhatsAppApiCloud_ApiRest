package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage;

//* Usado por --> ResponseWhatsapp.java

public record ResponseWhatsappContact(
        String input,
        String wa_id
) {
}
