package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.TemplateMessageDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.media.ResponseMediaMetadata;
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
     Boolean deleteMediaById(String mediaId) throws IOException;
     ResponseWhatsapp sendImageMessageById(String toPhoneNumber, String mediaId, String caption) throws JsonProcessingException;
     ResponseWhatsapp sendDocumentMessageById(String toPhoneNumber, String documentId, String caption, String filename);
     ResponseMediaMetadata getMediaMetadata(String mediaId);
     ResponseWhatsapp sendTemplatefeedback(String toPhoneNumber);
     Page<TemplateMessageDto> getResponsesTemplate(Pageable pageable, Boolean onlyAnswered);
     List<TemplateMessageDto> listResponseTemplateByPhone(String WhatsAppPhone);
     List<TemplateMessageDto> listResponseTemplateByDate(LocalDateTime inicio, LocalDateTime fin);
     List<TemplateMessageDto> listResponseTemplateByName(String templateName);

}
