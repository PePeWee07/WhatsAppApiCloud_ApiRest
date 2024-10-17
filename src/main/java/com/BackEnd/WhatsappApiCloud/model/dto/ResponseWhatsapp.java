package com.BackEnd.WhatsappApiCloud.model.dto;

import java.util.List;

public record ResponseWhatsapp(
        String messaging_product,
        List<ResponseWhatsappContact> contacts,
        List<ResponseWhatsappMessage> messages
) {
}