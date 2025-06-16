package com.BackEnd.WhatsappApiCloud.service.userChat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.domain.Page;

import com.BackEnd.WhatsappApiCloud.model.dto.user.UserChatFullDto;
import com.fasterxml.jackson.core.JsonProcessingException;
public interface UserchatService {
    UserChatFullDto userUpdate(Long id, Map<String, Object> updates);
    Page<UserChatFullDto> usersTable(int page, int size, String sortBy, String direction);
    Page<UserChatFullDto> tablefindByLastInteraction(int page, int size, String sortBy, String direction, LocalDateTime inicio, LocalDateTime fin);
    Page<UserChatFullDto> tablefindByChatSessionStart(int page, int size, String sortBy, String direction, LocalDateTime inicio, LocalDateTime fin);
    UserChatFullDto findByIdentificacion(String identificacion);
    UserChatFullDto findByWhatsappPhone(String whatsappPhone);
    void userRequestTicketInfo(String whatsAppPhone, String ticketId) throws JsonProcessingException, IOException;
    void userRequestTicketList(String whatsAppPhone) throws JsonProcessingException;
}
