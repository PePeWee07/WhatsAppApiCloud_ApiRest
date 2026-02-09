package com.BackEnd.WhatsappApiCloud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.BackEnd.WhatsappApiCloud.model.entity.reports.TicketReportEntity;

@Repository
public interface TicketReportRepository extends JpaRepository<TicketReportEntity, Long> {
    
}
