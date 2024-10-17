package com.BackEnd.WhatsappApiCloud.service.api;

import com.BackEnd.WhatsappApiCloud.model.dto.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.entity.MessageBody;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface ApiWhatsappService {

     ResponseWhatsapp sendMessage(MessageBody payload) throws JsonProcessingException;
}
