package com.BackEnd.WhatsappApiCloud.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageRowView;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageEntity;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    Optional<MessageEntity> findByWamid(String wamid);

    @Query("""
        SELECT
          m.id as id,
          m.wamid as wamid,
          m.profileName as profileName,
          m.conversationUserPhone as conversationUserPhone,
          m.fromPhone as fromPhone,
          m.toPhone as toPhone,
          cast(m.direction as string) as direction,
          cast(m.source as string) as source,
          cast(m.type as string) as type,
          m.timestamp as timestamp,
          m.sentAt as sentAt,
          m.deliveredAt as deliveredAt,
          m.readAt as readAt,
          m.failedAt as failedAt,
          m.mediaId as mediaId,
          m.mediaMimeType as mediaMimeType,
          m.mediaFilename as mediaFilename,
          m.mediaCaption as mediaCaption,
          m.relatedWamid as relatedWamid,
          m.reactionEmoji as reactionEmoji,
          m.textBody as textBody,

          (select count(ar) from AiResponseEntity ar where ar.message = m) as aiResponseCount,

          case when mt.id is null then false else true end as hasTemplate,
          case when mp.id is null then false else true end as hasPricing,
          case when ma.id is null then false else true end as hasAddres,
          case when me.id is null then false else true end as hasError

        FROM MessageEntity m
        LEFT JOIN m.messageTemplate mt
        LEFT JOIN m.messagePricingEntity mp
        LEFT JOIN m.messageAddresEntity ma
        LEFT JOIN m.messageErrorEntity me
        WHERE m.conversationUserPhone = :phone
    """)
    Page<MessageRowView> findHistoryByPhone(@Param("phone") String phone, Pageable pageable);

}
