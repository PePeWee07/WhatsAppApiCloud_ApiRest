package com.BackEnd.WhatsappApiCloud.service.userChat;

import com.BackEnd.WhatsappApiCloud.model.entity.User.UserChatEntity;


public interface UserchatService {
    
    UserChatEntity findByCedula(String cedula);
    UserChatEntity findByPhone(String phone);
}
