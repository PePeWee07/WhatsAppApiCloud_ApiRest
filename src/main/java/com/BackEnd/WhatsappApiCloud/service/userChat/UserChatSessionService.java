package com.BackEnd.WhatsappApiCloud.service.userChat;

import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatSessionEntity;

public interface UserChatSessionService {
    UserChatSessionEntity createSessionIfNotExists(String whatsappPhone);
} 
