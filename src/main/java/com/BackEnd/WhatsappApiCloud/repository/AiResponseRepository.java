package com.BackEnd.WhatsappApiCloud.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.BackEnd.WhatsappApiCloud.model.entity.openIA.AiResponseEntity;

@Repository
public interface AiResponseRepository extends JpaRepository<AiResponseEntity, Long> {
    List<AiResponseEntity> findByMessageIdOrderByCreatedAtAsc(Long messageId);
}
