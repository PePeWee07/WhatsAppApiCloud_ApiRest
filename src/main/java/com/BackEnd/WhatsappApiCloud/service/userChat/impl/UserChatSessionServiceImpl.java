package com.BackEnd.WhatsappApiCloud.service.userChat.impl;

import java.time.LocalDateTime;
import java.util.Optional;

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

        LocalDateTime now = LocalDateTime.now();
        // Rango del día actual: 00:00 -> 00:00 del día siguiente
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        Optional<UserChatSessionEntity> op = chatSessionRepository
                .findFirstByWhatsappPhoneAndStartTimeBetweenOrderByStartTimeDesc(
                        whatsappPhone, startOfDay, endOfDay);

        if (op.isPresent()) {
            UserChatSessionEntity session = op.get();
            session.setMessageCount(session.getMessageCount() + 1);
            return chatSessionRepository.save(session);
        }

        UserChatSessionEntity session = new UserChatSessionEntity();
        session.setWhatsappPhone(whatsappPhone);
        session.setStartTime(now);
        session.setMessageCount(1);
        return chatSessionRepository.save(session);
    }

}
