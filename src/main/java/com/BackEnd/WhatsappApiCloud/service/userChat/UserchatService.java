package com.BackEnd.WhatsappApiCloud.service.userChat;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.domain.Page;
import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatEntity;
public interface UserchatService {
    UserChatEntity patchUser(Long id, Map<String, Object> updates);
    Page<UserChatEntity> findAll(int page, int size, String sortBy, String direction);
    Page<UserChatEntity> findByLastInteraction(
        int page, int size,
        String sortBy, String direction,
        LocalDateTime inicio, LocalDateTime fin);
    UserChatEntity findByCedula(String cedula);
    UserChatEntity findByPhone(String phone);
}
