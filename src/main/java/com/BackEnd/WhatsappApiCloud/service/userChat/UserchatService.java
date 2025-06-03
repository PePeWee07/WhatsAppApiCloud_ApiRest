package com.BackEnd.WhatsappApiCloud.service.userChat;

import java.util.Map;

import org.springframework.data.domain.Page;

import com.BackEnd.WhatsappApiCloud.model.dto.user.UserChatFullDto;
public interface UserchatService {
    UserChatFullDto patchUser(Long id, Map<String, Object> updates);
    Page<UserChatFullDto> findAll(int page, int size, String sortBy, String direction);
    UserChatFullDto findByIdentificacion(String identificacion);
    UserChatFullDto findByWhatsappPhone(String whatsappPhone);
}
