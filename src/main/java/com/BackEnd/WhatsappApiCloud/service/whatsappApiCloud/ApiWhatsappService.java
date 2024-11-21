package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.webhookEvents.WhatsAppData;
import com.BackEnd.WhatsappApiCloud.model.entity.UserChatEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface ApiWhatsappService {

     ResponseWhatsapp sendMessage(MessageBody payload) throws JsonProcessingException;
     ResponseWhatsapp ResponseMessage(WhatsAppData.WhatsAppMessage message) throws JsonProcessingException;
     UserChatEntity guardarUsuario(UserChatEntity usuario);

}
