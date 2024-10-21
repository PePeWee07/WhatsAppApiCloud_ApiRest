package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage;

import java.util.List;

// * Respuesta al enviar un mensaje

public record ResponseWhatsapp(
        String messaging_product,
        List<ResponseWhatsappContact> contacts,
        List<ResponseWhatsappMessage> messages
) {
}