package com.BackEnd.WhatsappApiCloud.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.TemplateMessageEntity;

public interface TemplateMessageRepository extends JpaRepository<TemplateMessageEntity, Long> {
    Optional<TemplateMessageEntity> findByWamid(String wamid);
    List<TemplateMessageEntity> findByToPhone(String toPhone);
    Page<TemplateMessageEntity> findAll(Pageable pageable);
}