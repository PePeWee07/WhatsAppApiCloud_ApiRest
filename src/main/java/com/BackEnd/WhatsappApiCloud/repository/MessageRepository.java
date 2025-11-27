package com.BackEnd.WhatsappApiCloud.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageEntity;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    Optional<MessageEntity> findByMessageId(String messageId);
  
} 
