package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.webhookEvents.WhatsAppData;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface ApiWhatsappService {

     // ======================================================
     //   Funciones disponibles para el servicio de WhatsApp
     // ======================================================
     ResponseWhatsapp sendMessage(MessageBody payload) throws JsonProcessingException;
     ResponseWhatsapp handleUserMessage(WhatsAppData.WhatsAppMessage message) throws JsonMappingException, JsonProcessingException;

}
