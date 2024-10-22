package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.webhookEvents;

import java.util.List;

public class WhatsAppData {

    public record WhatsAppMessage(
        String object,
        List<Entry> entry
    ) {}

    public record Entry(
        String id,
        List<Change> changes
    ) {}

    public record Change(
        Value value,
        String field
    ) {}

    public record Value(
        String messaging_product,
        Metadata metadata,
        List<Contact> contacts,  // Para mensajes entrantes
        List<Message> messages,  // Para mensajes entrantes y posiblemente para eventos relacionados con mensajes
        List<Status> statuses    // Para actualizaciones de estado de mensajes enviados
    ) {}

    public record Metadata(
        String display_phone_number,
        String phone_number_id
    ) {}

    public record Contact(
        Profile profile,
        String wa_id
    ) {}

    public record Profile(
        String name
    ) {}

    public record Message(
        String from,     // El remitente del mensaje, útil para mensajes entrantes
        String id,
        String timestamp,
        Text text,
        String type
    ) {}

    public record Text(
        String body  // Cuerpo del mensaje
    ) {}

    public record Status(
        String id,
        String status,  // Estado del mensaje, como 'delivered'
        String timestamp,
        String recipient_id,
        Conversation conversation,
        Pricing pricing
    ) {}

    public record Conversation(
        String id,
        Origin origin
    ) {}

    public record Origin(
        String type  // Tipo de origen, útil para clasificar la conversación
    ) {}

    public record Pricing(
        boolean billable,    // Si el mensaje es facturable
        String pricing_model,
        String category      // Categoría del servicio de mensajería
    ) {}
}
