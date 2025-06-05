package com.BackEnd.WhatsappApiCloud.service.userChat;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.domain.Page;

import com.BackEnd.WhatsappApiCloud.model.dto.user.UserChatFullDto;
public interface UserchatService {
    UserChatFullDto userUpdate(Long id, Map<String, Object> updates);
    Page<UserChatFullDto> usersTable(int page, int size, String sortBy, String direction);
    Page<UserChatFullDto> tablefindByLastInteraction(int page, int size, String sortBy, String direction, LocalDateTime inicio, LocalDateTime fin);
    UserChatFullDto findByIdentificacion(String identificacion);
    UserChatFullDto findByWhatsappPhone(String whatsappPhone);
}
