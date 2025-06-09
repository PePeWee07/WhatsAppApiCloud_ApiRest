package com.BackEnd.WhatsappApiCloud.service.glpi;

import com.BackEnd.WhatsappApiCloud.model.dto.glpi.TicketInfoDto;

public interface GlpiService {

    TicketInfoDto getInfoTicketById(String ticketId);
    
}
