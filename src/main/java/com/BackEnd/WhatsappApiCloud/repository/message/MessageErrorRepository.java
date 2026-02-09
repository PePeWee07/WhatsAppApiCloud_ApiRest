package com.BackEnd.WhatsappApiCloud.repository.message;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageErrorEntity;

@Repository
public interface MessageErrorRepository extends JpaRepository<MessageErrorEntity, Long> {
    Optional<MessageErrorEntity> findByMessageId(Long messageId);
    
}
