package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface ApiWhatsappService {

     ResponseWhatsapp sendMessage(MessageBody payload) throws JsonProcessingException;
}
