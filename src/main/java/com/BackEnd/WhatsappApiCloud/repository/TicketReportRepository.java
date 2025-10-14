package com.BackEnd.WhatsappApiCloud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.BackEnd.WhatsappApiCloud.model.entity.reports.TicketReportEntity;

public interface TicketReportRepository extends JpaRepository<TicketReportEntity, Long> {
    
}
