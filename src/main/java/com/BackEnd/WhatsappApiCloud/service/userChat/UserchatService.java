package com.BackEnd.WhatsappApiCloud.service.userChat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import com.BackEnd.WhatsappApiCloud.model.dto.glpi.SolutionDecisionRequest;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.TicketInfoDto;
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
    TicketInfoDto userRequestTicketInfo(String whatsAppPhone, Long ticketId) throws JsonProcessingException, IOException;
    List<UserTicketDto> userRequestTicketList(String whatsAppPhone) throws JsonProcessingException;
    Object setWaitingAttachmentsState(String whatsappPhone);
    Object setWaitingAttachmentsStateForExistingTicket(String whatsappPhone, Long ticketId);
    Object createNoteForTicket( Long ticketId, String contentNote, String whatsAppPhone);
    Object refusedOrAcceptedSolutionTicket(SolutionDecisionRequest request, String whatsAppPhone);
    void closeAttachmentSession(String whatsappPhone);
}
