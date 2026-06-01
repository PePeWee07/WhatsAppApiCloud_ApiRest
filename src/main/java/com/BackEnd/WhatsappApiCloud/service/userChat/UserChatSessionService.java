package com.BackEnd.WhatsappApiCloud.service.userChat;

import java.util.List;

import com.BackEnd.WhatsappApiCloud.model.dto.user.UserChatSessionDto;
import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatSessionEntity;

public interface UserChatSessionService {
    UserChatSessionEntity createSessionIfNotExists(String whatsappPhone);
    List<UserChatSessionDto> listSessions(String whatsappPhone);
}
