package com.BackEnd.WhatsappApiCloud.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.BackEnd.WhatsappApiCloud.model.entity.user.ChatToolCallEntity;

public interface ChatToolCallRepository extends JpaRepository<ChatToolCallEntity, Long> {
    Optional<ChatToolCallEntity> findByCallId(String callId);
}
