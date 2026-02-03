package com.BackEnd.WhatsappApiCloud.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessagePricingEntity;

public interface MessagePricingRepository extends JpaRepository<MessagePricingEntity, Long> {
    Optional<MessagePricingEntity> findByMessageId(Long messageId);
}
