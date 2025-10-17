package com.BackEnd.WhatsappApiCloud.service.userChat.impl;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatSessionEntity;
import com.BackEnd.WhatsappApiCloud.repository.UserChatSessionRepository;
import com.BackEnd.WhatsappApiCloud.service.userChat.UserChatSessionService;

@Service
public class UserChatSessionServiceImpl implements UserChatSessionService {

    /**
     * Verifica si existe una sesión activa para el número en las últimas 'sessionDurationHours' horas.
     * Si no existe, crea y guarda una nueva sesión.
     *
     * @param phone    Número de teléfono del usuario
     * @return La sesión de chat activa (nueva o existente)
     */

    private final UserChatSessionRepository chatSessionRepository;

    public UserChatSessionServiceImpl(UserChatSessionRepository chatSessionRepository) {
        this.chatSessionRepository = chatSessionRepository;
    }
    
    @Override
    public UserChatSessionEntity createSessionIfNotExists(String whatsappPhone) {
        int sessionDurationHours = 24;
        LocalDateTime now       = LocalDateTime.now();
        LocalDateTime threshold = now.minusHours(sessionDurationHours);

        // 1) Buscar sesiones activas para este userChatId
        List<UserChatSessionEntity> active = chatSessionRepository.findByWhatsappPhoneAndStartTimeBetween(whatsappPhone, threshold, now);

        if (!active.isEmpty()) {
            // 2) Existe actualizar contador
            UserChatSessionEntity session = active.get(0);
            session.setMessageCount(session.getMessageCount() + 1);
            return chatSessionRepository.save(session);
        } else {
            // 3) No existe crear nueva
            UserChatSessionEntity session = new UserChatSessionEntity();
            session.setWhatsappPhone(whatsappPhone);
            session.setStartTime(now);
            session.setEndTime(now.plusHours(sessionDurationHours));
            session.setMessageCount(1);
            return chatSessionRepository.save(session);
        }
    }

}
