package com.BackEnd.WhatsappApiCloud.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.BackEnd.WhatsappApiCloud.config.ApiKeyFilter;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.webhookEvents.WhatsAppData;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.ApiWhatsappService;
import com.fasterxml.jackson.core.JsonProcessingException;

@RestController
@RequestMapping("/api/v1/whatsapp")
public class WhatsappController {

    private final ApiWhatsappService apiWhatsappService;

    public WhatsappController(ApiWhatsappService apiWhatsappService) {
        this.apiWhatsappService = apiWhatsappService;
    }

    @Autowired
    private ApiKeyFilter apiKeyFilter;

    
    // ======================================================
    //   Enviar mensaje a un usuario de WhatsApp especifico
    // ======================================================
    @PostMapping("/send")
    ResponseWhatsapp enviar(@RequestBody MessageBody payload) throws JsonProcessingException {
            return apiWhatsappService.sendMessage(payload);
    }


    // ======================================================
    //   Recibir mensaje de un usuario de WhatsApp
    // ======================================================
    @PostMapping("/receive")
    public ResponseWhatsapp receiveMessage(@RequestBody WhatsAppData.WhatsAppMessage message) throws JsonProcessingException {
        if(message.entry().get(0).changes().get(0).value().messages() != null){
            System.out.println("Mensaje recibido: " + message.entry().get(0).changes().get(0).value().messages().get(0).text()); //! Debug
            apiKeyFilter.getPhoneNumber(message.entry().get(0).changes().get(0).value().contacts().get(0).wa_id());
            return apiWhatsappService.handleUserMessage(message);
        }
        return null;
    }
}
