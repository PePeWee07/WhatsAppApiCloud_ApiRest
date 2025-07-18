package com.BackEnd.WhatsappApiCloud.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.BackEnd.WhatsappApiCloud.model.entity.user.ChatTurnEntity;

public interface ChatTurnRepository extends JpaRepository<ChatTurnEntity, Long> {
    @EntityGraph(attributePaths = {"messages", "toolCalls"})
    List<ChatTurnEntity> findByWhatsappPhoneOrderByCreatedAtDesc(String whatsappPhone);
}
