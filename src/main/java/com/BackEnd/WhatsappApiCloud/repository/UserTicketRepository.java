package com.BackEnd.WhatsappApiCloud.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.BackEnd.WhatsappApiCloud.model.entity.glpi.UserTicketEntity;

@Repository
public interface UserTicketRepository extends JpaRepository<UserTicketEntity, Long> {
    /**
     * Busca todos los tickets de un whatsappPhone dado
     * cuyo campo status sea distinto de "Cerrado".
     */
    List<UserTicketEntity> findByWhatsappPhoneAndStatusNot(String whatsappPhone, String status);

    // Verifico que el ticket pertenesca al usaurio
    boolean existsByWhatsappPhoneAndId(String whatsappPhone, Long id);
}
