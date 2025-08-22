package com.BackEnd.WhatsappApiCloud.service.glpi;

import com.BackEnd.WhatsappApiCloud.model.dto.glpi.GlpiDto.CreateTicket;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.TicketInfoDto;

public interface GlpiService {

    TicketInfoDto getInfoTicketById(Long ticketId);
    Object createTicket(CreateTicket ticket, String whatsAppPhone);
    String getStatusTicket(Long tickedId);
    void attachRecentWhatsappMediaToTicket(String waId, long ticketId, int minutesWindow);
}
