package com.BackEnd.WhatsappApiCloud.service.glpi;

import com.BackEnd.WhatsappApiCloud.model.dto.glpi.GlpiDto.CreateTicket;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.GlpiDto.responseCreateTicketSuccess;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.SolutionDecisionRequest;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.TicketInfoDto;

public interface GlpiService {

    TicketInfoDto getInfoTicketById(String ticketId);
    responseCreateTicketSuccess createTicket(CreateTicket ticket, String whatsAppPhone);
    String getStatusTicket(Long tickedId);
    Object refusedOrAcceptedSolutionTicket(SolutionDecisionRequest request, String whatsAppPhone);
}
