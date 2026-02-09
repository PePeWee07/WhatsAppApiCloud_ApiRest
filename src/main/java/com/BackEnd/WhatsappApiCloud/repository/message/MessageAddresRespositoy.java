package com.BackEnd.WhatsappApiCloud.repository.message;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageAddresEntity;

public interface MessageAddresRespositoy extends JpaRepository<MessageAddresEntity, Long> {
    Optional<MessageAddresEntity> findByMessageId(Long messageId);
    
} 
