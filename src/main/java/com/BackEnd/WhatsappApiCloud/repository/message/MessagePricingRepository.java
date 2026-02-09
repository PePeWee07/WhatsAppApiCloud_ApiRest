package com.BackEnd.WhatsappApiCloud.repository.message;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessagePricingEntity;

@Repository
public interface MessagePricingRepository extends JpaRepository<MessagePricingEntity, Long> {
    Optional<MessagePricingEntity> findByMessageId(Long messageId);
}
