package com.BackEnd.WhatsappApiCloud.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageTemplateEntity;

public interface TemplateMessageRepository extends JpaRepository<MessageTemplateEntity, Long> {
    Page<MessageTemplateEntity> findAll(Pageable pageable);
    Page<MessageTemplateEntity> findByAnswerIsNotNullAndAnswerNot(Pageable pageable, String value);
    List<MessageTemplateEntity> findByTemplateName(String templateName);
    Optional<MessageTemplateEntity> findByMessage_MessageId(String messageId);
    List<MessageTemplateEntity> findByMessage_ToPhone(String toPhone);
    List<MessageTemplateEntity> findByMessage_TimestampBetween(Instant start, Instant end);
}