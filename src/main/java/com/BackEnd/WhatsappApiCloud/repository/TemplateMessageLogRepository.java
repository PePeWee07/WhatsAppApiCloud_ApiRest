package com.BackEnd.WhatsappApiCloud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.TemplateMessageLog;

public interface TemplateMessageLogRepository extends JpaRepository<TemplateMessageLog, Long> {
}