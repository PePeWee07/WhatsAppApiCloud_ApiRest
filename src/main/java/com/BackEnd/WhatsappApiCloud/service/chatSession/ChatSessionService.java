package com.BackEnd.WhatsappApiCloud.service.chatSession;

import com.BackEnd.WhatsappApiCloud.model.entity.user.ChatSessionEntity;

public interface ChatSessionService {
    ChatSessionEntity createSessionIfNotExists(String whatsappPhone);
} 
