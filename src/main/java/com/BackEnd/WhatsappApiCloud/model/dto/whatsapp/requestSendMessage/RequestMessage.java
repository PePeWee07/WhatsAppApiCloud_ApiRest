package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage;

//* Estrucutra de la Api para enviar un mensaje --> (Messages/Send Text Message)

public record RequestMessage(
    String messaging_product,
    String recipient_type,
    String to,
    String type,
    RequestMessageText text
) {
} 
