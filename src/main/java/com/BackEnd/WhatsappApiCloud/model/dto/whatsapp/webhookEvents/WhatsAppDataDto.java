package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.webhookEvents;

import java.util.List;
import java.util.Optional;

public class WhatsAppDataDto {

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
        List<Contact> contacts,
        List<Message> messages,
        List<Status> statuses
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
        String from,
        String id,
        String timestamp,
        Optional<Text> text,
        String type,
        Optional<Document> document,
        Optional<Image> image,
        Optional<Sticker> sticker,
        Optional<Reaction> reaction,
        Optional<Location> location,
        Object interactive,
        Context context
    ) {}

    public record Context (
        String from,
        String id,
        Optional<ReferredProduct> referred_product
    ) {}

    public record ReferredProduct(
        String catalog_id,
        String product_retailer_id
    ) {}

    public record Text(
        String body
    ) {}

    public record Image(
        String caption,
        String mime_type,
        String sha256,
        String id
    ) {}

    public record Document(
        String caption,
        String filename,
        String mime_type,
        String sha256,
        String id
    ) {}

    public record Reaction(
        String message_id,
        String emoji
    ) {}

    public record Location(
        String latitude,
        String longitude,
        String name,
        String address
    ) {}

    public record Sticker(
        String mime_type,
        String sha256,
        String id
    ) {}

    public record Status(
        String id,
        String status,
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
        String type
    ) {}

    public record Pricing(
        boolean billable,
        String pricing_model,
        String category
    ) {}
}
