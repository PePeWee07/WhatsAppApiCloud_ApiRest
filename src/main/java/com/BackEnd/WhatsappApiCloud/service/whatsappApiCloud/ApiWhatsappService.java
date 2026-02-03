package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseMediaMetadata;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseMessageTemplate;
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
     void handleMessageStatus(WhatsAppDataDto.WhatsAppMessage status);
     Boolean deleteMediaById(String mediaId) throws IOException;
     ResponseWhatsapp sendImageMessageById(MessageBody payload, String mediaId);
     ResponseWhatsapp sendImageMessageByUrl(MessageBody payload, String imageUrl);
     ResponseWhatsapp sendVideoMessageById(MessageBody payload, String videoId);
     ResponseWhatsapp sendVideoMessageByUrl(MessageBody payload, String videoUrl);
     ResponseWhatsapp sendDocumentMessageById(MessageBody payload, String documentId, String filename);
     ResponseWhatsapp sendDocumentMessageByUrl(MessageBody payload, String documentUrl, String filename);
     ResponseMediaMetadata getMediaMetadata(String mediaId);
     ResponseWhatsapp sendTemplatefeedback(String toPhoneNumber);
     Page<ResponseMessageTemplate> getResponsesTemplate(Pageable pageable, Boolean onlyAnswered);
     List<ResponseMessageTemplate> listResponseTemplateByPhone(String WhatsAppPhone);
     List<ResponseMessageTemplate> listResponseTemplateByDate(LocalDateTime inicio, LocalDateTime fin);
     List<ResponseMessageTemplate> listResponseTemplateByName(String templateName);

}
