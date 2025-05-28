package com.BackEnd.WhatsappApiCloud.service.chatSession.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.BackEnd.WhatsappApiCloud.model.entity.user.ChatSession;
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

    @Autowired
    private ChatSessionRepository chatSessionRepository;
    
     public ChatSession createSessionIfNotExists(String phone) {

        int sessionDurationHours  = 24;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusHours(sessionDurationHours);
        
        // Buscamos sesiones activas para ese teléfono iniciadas en las últimas 'sessionDurationHours' horas
        List<ChatSession> activeSessions = chatSessionRepository.findByPhoneAndStartTimeBetween(phone, threshold, now);
        
        if (!activeSessions.isEmpty()) {
            ChatSession activeSession = activeSessions.get(0);
            activeSession.setMessageCount(activeSession.getMessageCount() + 1);
            return chatSessionRepository.save(activeSession);
        } else {
            ChatSession newSession = new ChatSession();
            newSession.setPhone(phone);
            newSession.setStartTime(now);
            newSession.setEndTime(now.plusHours(sessionDurationHours));
            newSession.setMessageCount(0);
            return chatSessionRepository.save(newSession);
        }
    }

}
