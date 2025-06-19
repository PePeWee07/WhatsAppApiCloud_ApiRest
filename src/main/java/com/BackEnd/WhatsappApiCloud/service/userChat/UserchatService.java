package com.BackEnd.WhatsappApiCloud.service.userChat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import com.BackEnd.WhatsappApiCloud.model.dto.user.UserChatFullDto;
import com.BackEnd.WhatsappApiCloud.model.dto.user.UserTicketDto;
import com.fasterxml.jackson.core.JsonProcessingException;
public interface UserchatService {
    UserChatFullDto userUpdate(Long id, Map<String, Object> updates);
    Page<UserChatFullDto> usersTable(int page, int size, String sortBy, String direction);
    Page<UserChatFullDto> tablefindByLastInteraction(int page, int size, String sortBy, String direction, LocalDateTime inicio, LocalDateTime fin);
    Page<UserChatFullDto> tablefindByChatSessionStart(int page, int size, String sortBy, String direction, LocalDateTime inicio, LocalDateTime fin);
    UserChatFullDto findByIdentificacion(String identificacion);
    UserChatFullDto findByWhatsappPhone(String whatsappPhone);
    boolean userRequestTicketInfo(String whatsAppPhone, String ticketId) throws JsonProcessingException, IOException;
    List<UserTicketDto> userRequestTicketList(String whatsAppPhone) throws JsonProcessingException;
}
