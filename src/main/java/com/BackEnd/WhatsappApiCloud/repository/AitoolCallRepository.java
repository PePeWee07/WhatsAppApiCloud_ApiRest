package com.BackEnd.WhatsappApiCloud.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.BackEnd.WhatsappApiCloud.model.entity.openIA.AiToolCallEntity;

@Repository
public interface AitoolCallRepository extends JpaRepository<AiToolCallEntity, Long> {
    Optional<AiToolCallEntity> findByCallId(String callId);
    
    List<AiToolCallEntity> findByAiResponseIdOrderByIdAsc(Long aiResponseId);
}
