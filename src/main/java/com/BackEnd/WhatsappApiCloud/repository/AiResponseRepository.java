package com.BackEnd.WhatsappApiCloud.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.BackEnd.WhatsappApiCloud.model.entity.openIA.AiResponseEntity;


public interface AiResponseRepository extends JpaRepository<AiResponseEntity, Long> {
}
