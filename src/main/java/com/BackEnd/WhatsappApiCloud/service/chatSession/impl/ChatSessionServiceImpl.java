package com.BackEnd.WhatsappApiCloud.service.chatSession.impl;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

import com.BackEnd.WhatsappApiCloud.model.entity.user.ChatSessionEntity;
import com.BackEnd.WhatsappApiCloud.repository.ChatSessionRepository;
import com.BackEnd.WhatsappApiCloud.service.chatSession.ChatSessionService;

@Service
public class ChatSessionServiceImpl implements ChatSessionService {

    /**
     * Verifica si existe una sesión activa para el número en las últimas 'sessionDurationHours' horas.
     * Si no existe, crea y guarda una nueva sesión.
     *
     * @param phone    Número de teléfono del usuario
     * @return La sesión de chat activa (nueva o existente)
     */

    private final ChatSessionRepository chatSessionRepository;

    public ChatSessionServiceImpl(ChatSessionRepository chatSessionRepository) {
        this.chatSessionRepository = chatSessionRepository;
    }
    
    @Override
    public ChatSessionEntity createSessionIfNotExists(String whatsappPhone) {
        int sessionDurationHours = 24;
        LocalDateTime now       = LocalDateTime.now();
        LocalDateTime threshold = now.minusHours(sessionDurationHours);

        // 1) Buscar sesiones activas para este userChatId
        List<ChatSessionEntity> active = chatSessionRepository.findByWhatsappPhoneAndStartTimeBetween(whatsappPhone, threshold, now);

        if (!active.isEmpty()) {
            // 2) Existe → actualizar contador
            ChatSessionEntity session = active.get(0);
            session.setMessageCount(session.getMessageCount() + 1);
            return chatSessionRepository.save(session);
        } else {
            // 3) No existe → crear nueva
            ChatSessionEntity session = new ChatSessionEntity();
            session.setWhatsappPhone(whatsappPhone);
            session.setStartTime(now);
            session.setEndTime(now.plusHours(sessionDurationHours));
            session.setMessageCount(0);
            return chatSessionRepository.save(session);
        }
    }

}
