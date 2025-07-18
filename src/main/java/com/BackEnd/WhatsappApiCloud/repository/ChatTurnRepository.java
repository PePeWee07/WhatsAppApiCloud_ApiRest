package com.BackEnd.WhatsappApiCloud.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.BackEnd.WhatsappApiCloud.model.entity.user.ChatTurnEntity;

public interface ChatTurnRepository extends JpaRepository<ChatTurnEntity, Long> {
    @EntityGraph(attributePaths = {"messages", "toolCalls"})
    Page<ChatTurnEntity> findByWhatsappPhoneOrderByCreatedAtDesc(String whatsappPhone, Pageable pageable);

}
