package com.BackEnd.WhatsappApiCloud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatSessionEntity;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface UserChatSessionRepository extends JpaRepository<UserChatSessionEntity, Long> {
    
    // Ejemplo de método para obtener sesiones de un usuario en un rango de tiempo
    List<UserChatSessionEntity>findByWhatsappPhoneAndStartTimeBetween(
        String whatsappPhone, 
        LocalDateTime startTime, 
        LocalDateTime endTime
    );
}
