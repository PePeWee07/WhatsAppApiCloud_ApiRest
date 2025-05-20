package com.BackEnd.WhatsappApiCloud.service.userChat;

import org.springframework.data.domain.Page;
import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatEntity;
public interface UserchatService {
    Page<UserChatEntity> findAll(int page, int size, String sortBy, String direction);
    UserChatEntity findByCedula(String cedula);
    UserChatEntity findByPhone(String phone);
}
