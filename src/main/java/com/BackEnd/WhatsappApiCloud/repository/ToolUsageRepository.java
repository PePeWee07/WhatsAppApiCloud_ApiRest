package com.BackEnd.WhatsappApiCloud.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.BackEnd.WhatsappApiCloud.model.entity.tool.ToolUsageEntity;

@Repository
public interface ToolUsageRepository extends JpaRepository<ToolUsageEntity, Long> {
    List<ToolUsageEntity> findByWhatsappPhone(String whatsappPhone);
    Optional<ToolUsageEntity> findByWhatsappPhoneAndToolName(String whatsappPhone, String toolName);
}
