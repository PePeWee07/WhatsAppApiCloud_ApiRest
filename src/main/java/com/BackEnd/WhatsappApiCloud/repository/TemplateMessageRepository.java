package com.BackEnd.WhatsappApiCloud.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageTemplateEntity;

public interface TemplateMessageRepository extends JpaRepository<MessageTemplateEntity, Long> {
    List<MessageTemplateEntity> findByTemplateName(String templateName);
    
    Optional<MessageTemplateEntity> findByMessageWamid(String wamid);

    List<MessageTemplateEntity> findByMessageToPhone(String toPhone);
    List<MessageTemplateEntity> findByMessageTimestampBetween(Instant start, Instant end);

    @Query("SELECT mt FROM MessageTemplateEntity mt JOIN mt.message m")
    Page<MessageTemplateEntity> findAllWithMessages(Pageable pageable);
    @Query("SELECT mt FROM MessageTemplateEntity mt JOIN mt.message m WHERE mt.answer IS NOT NULL AND mt.answer <> ''")
    Page<MessageTemplateEntity> findAnsweredWithMessages(Pageable pageable);
}