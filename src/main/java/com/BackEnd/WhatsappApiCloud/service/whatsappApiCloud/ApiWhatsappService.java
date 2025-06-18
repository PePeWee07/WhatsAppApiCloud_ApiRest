package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud;

import java.io.File;
import java.io.IOException;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.webhookEvents.WhatsAppDataDto;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface ApiWhatsappService {

     // ======================================================
     //   Funciones disponibles para el servicio de WhatsApp
     // ======================================================
     ResponseWhatsapp sendMessage(MessageBody payload) throws JsonProcessingException;
     ResponseWhatsapp handleUserMessage(WhatsAppDataDto.WhatsAppMessage message) throws JsonMappingException, JsonProcessingException;
     String uploadMedia(File mediaFile) throws IOException;
     Boolean deleteMediaById(String mediaId) throws IOException;
     ResponseWhatsapp sendImageMessageById(String toPhoneNumber, String mediaId, String caption) throws JsonProcessingException;
     ResponseWhatsapp sendDocumentMessageById(String toPhoneNumber, String documentId, String caption, String filename);

}
