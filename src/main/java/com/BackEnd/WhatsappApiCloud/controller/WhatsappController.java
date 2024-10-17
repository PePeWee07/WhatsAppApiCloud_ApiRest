package com.BackEnd.WhatsappApiCloud.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.BackEnd.WhatsappApiCloud.model.dto.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.entity.MessageBody;
import com.BackEnd.WhatsappApiCloud.service.api.ApiWhatsappService;
import com.fasterxml.jackson.core.JsonProcessingException;

@RestController
@RequestMapping("/api/v1/whatsapp")
public class WhatsappController {

    private final ApiWhatsappService apiWhatsappService;

    public WhatsappController(ApiWhatsappService apiWhatsappService) {
        this.apiWhatsappService = apiWhatsappService;
    }

    @PostMapping("/enviar")
    ResponseWhatsapp enviar(@RequestBody MessageBody payload) throws JsonProcessingException {
        return apiWhatsappService.sendMessage(payload);
    }
}
