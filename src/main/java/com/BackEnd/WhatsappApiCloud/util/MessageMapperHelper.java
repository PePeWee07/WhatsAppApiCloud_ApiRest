package com.BackEnd.WhatsappApiCloud.util;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.webhookEvents.WhatsAppDataDto;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageEntity;
import com.BackEnd.WhatsappApiCloud.util.enums.MessageDirectionEnum;
import com.BackEnd.WhatsappApiCloud.util.enums.MessageSourceEnum;
import com.BackEnd.WhatsappApiCloud.util.enums.MessageTypeEnum;

import java.time.*;

public class MessageMapperHelper {

    public static MessageEntity fromWebhookMessage(
            WhatsAppDataDto.Value value,
            WhatsAppDataDto.Message msg,
            MessageDirectionEnum direction,
            MessageSourceEnum source) {
        MessageEntity entity = new MessageEntity();

        // Datos básicos
        entity.setMessageId(msg.id());
        entity.setConversationUserPhone(value.contacts().get(0).wa_id());
        entity.setFromPhone(msg.from());
        entity.setToPhone(value.metadata().display_phone_number());
        entity.setDirection(direction);
        entity.setSource(source);
        entity.setType(MessageTypeEnum.valueOf(msg.type().toUpperCase()));

        // Timestamp
        try {
            long epoch = Long.parseLong(msg.timestamp());
            entity.setTimestamp(Instant.ofEpochSecond(epoch));
        } catch (Exception e) {
            entity.setTimestamp(Instant.now());
        }

        if (msg.context() != null && msg.context().id() != null) {
            entity.setRelatedMessageId(msg.context().id());
        }

        // Datos según tipo
        switch (msg.type()) {
            case "text" -> msg.text().ifPresent(t -> {
                entity.setTextBody(t.body());
            });

            case "image" -> msg.image().ifPresent(img -> {
                entity.setCaption(img.caption());
                entity.setMimeType(img.mime_type());
                entity.setMediaId(img.id());
            });

            case "document" -> msg.document().ifPresent(doc -> {
                entity.setCaption(doc.caption());
                entity.setMimeType(doc.mime_type());
                entity.setMediaId(doc.id());
                entity.setMediaFilename(doc.filename());
            });

            case "audio" -> msg.audio().ifPresent(aud -> {
                entity.setMimeType(aud.mime_type());
                entity.setMediaId(aud.id());
                entity.setCaption(aud.voice() ? "Nota de voz" : "Archivo de audio");
            });

            case "sticker" -> msg.sticker().ifPresent(st -> {
                entity.setMimeType(st.mime_type());
                entity.setMediaId(st.id());
            });

            case "reaction" -> msg.reaction().ifPresent(r -> {
                entity.setReactionEmoji(r.emoji());
                entity.setRelatedMessageId(r.message_id());
            });

            case "location" -> msg.location().ifPresent(loc -> {
                entity.setLatitude(Double.valueOf(loc.latitude()));
                entity.setLongitude(Double.valueOf(loc.longitude()));
                entity.setLocationName(loc.name());
                entity.setLocationAddress(loc.address());
            });

            case "video" -> msg.video().ifPresent(vid -> {
                entity.setCaption(vid.caption());
                entity.setMimeType(vid.mime_type());
                entity.setMediaId(vid.id());
            });

            case "unsupported" -> msg.unsupported().ifPresent(err -> {
                entity.setErrorCode(err.code());
                entity.setErrorTitle(err.title());
                entity.setErrorDetails(err.details());
            });

            // TODO: Manejar los contactos

            default -> {
                entity.setTextBody("[Tipo no manejado: " + msg.type() + "]");
                entity.setType(MessageTypeEnum.UNKNOWN);
            }
        }

        return entity;
    }

    public static MessageEntity createSentMessageEntity(MessageBody payload, ResponseWhatsapp response) {
        MessageEntity entity = new MessageEntity();
        entity.setConversationUserPhone(payload.number());
        entity.setFromPhone(payload.businessPhoneNumber());
        entity.setToPhone(payload.number());
        entity.setMessageId(response.messages().get(0).id());
        entity.setTimestamp(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setTextBody(payload.message());
        entity.setSource(payload.source());
        entity.setDirection(MessageDirectionEnum.OUTBOUND);
        entity.setProfileName(payload.sentBy());
        entity.setType(payload.type());
        if (payload.contextId() != null) {
            entity.setRelatedMessageId(payload.contextId());
        }
        return  entity;
    }
}
