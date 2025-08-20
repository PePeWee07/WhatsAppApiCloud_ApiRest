package com.BackEnd.WhatsappApiCloud.service.glpi;

import com.BackEnd.WhatsappApiCloud.model.dto.glpi.GlpiDto.CreateTicket;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.SolutionDecisionRequest;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.TicketInfoDto;

public interface GlpiService {

    TicketInfoDto getInfoTicketById(String ticketId);
    Object createTicket(CreateTicket ticket, String whatsAppPhone);
    String getStatusTicket(Long tickedId);
    Object refusedOrAcceptedSolutionTicket(SolutionDecisionRequest request, String whatsAppPhone);
    Object createNoteForTicket( Long ticketId, String contentNote, String whatsAppPhone);
    void attachRecentWhatsappMediaToTicket(String waId, long ticketId, int minutesWindow);
}
