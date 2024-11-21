package com.BackEnd.WhatsappApiCloud.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.webhookEvents.WhatsAppData;
import com.BackEnd.WhatsappApiCloud.model.entity.UserChatEntity;
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

    @PostMapping("/send")
    ResponseWhatsapp enviar(@RequestBody MessageBody payload) throws JsonProcessingException {
        return apiWhatsappService.sendMessage(payload);
    }

    @PostMapping("/receive")
    public ResponseWhatsapp receiveMessage(@RequestBody WhatsAppData.WhatsAppMessage message) throws JsonProcessingException {
        if(message.entry().get(0).changes().get(0).value().messages() != null){
            System.out.println("Mensaje recibido: " + message.entry().get(0).changes().get(0).value().messages().get(0).text());
            System.out.println(ResponseEntity.ok()); 
        }
        return apiWhatsappService.ResponseMessage(message);
    }

    @PostMapping("/guardarUsuario")
    public ResponseEntity<UserChatEntity> guardarUsuario(@RequestBody UserChatEntity usuario) {
        UserChatEntity usuarioGuardado = apiWhatsappService.guardarUsuario(usuario);
        return ResponseEntity.ok(usuarioGuardado);
    }
}
