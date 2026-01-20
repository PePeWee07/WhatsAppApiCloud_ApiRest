package com.BackEnd.WhatsappApiCloud.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessagePircingEntity;

public interface MessagePricingRepository extends JpaRepository<MessagePircingEntity, Long> {
    Optional<MessagePircingEntity> findByMessageId(Long messageId);
}
