package com.BackEnd.WhatsappApiCloud.service.chatSession;

import com.BackEnd.WhatsappApiCloud.model.entity.user.ChatSession;

public interface ChatSessionService {
    ChatSession createSessionIfNotExists(String whatsappPhone);
} 
