package com.BackEnd.WhatsappApiCloud.service.chatSession;

import com.BackEnd.WhatsappApiCloud.model.entity.User.ChatSession;

public interface ChatSessionService {
    ChatSession createSessionIfNotExists(String phone, String threadId);
} 
