package com.BackEnd.WhatsappApiCloud.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.BackEnd.WhatsappApiCloud.model.entity.openIA.AiToolCallEntity;

public interface AitoolCallRepository extends JpaRepository<AiToolCallEntity, Long> {
    Optional<AiToolCallEntity> findByCallId(String callId);
}
