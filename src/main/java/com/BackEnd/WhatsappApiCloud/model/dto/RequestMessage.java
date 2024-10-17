package com.BackEnd.WhatsappApiCloud.model.dto;

//* Estrucutra para enviar un mensaje --> (Messages/Send Text Message)

public record RequestMessage(
    String messaging_product,
    String recipient_type,
    String to,
    String type,
    RequestMessageText text
) {
} 
