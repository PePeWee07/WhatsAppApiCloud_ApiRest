package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestMessage;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestMessageText;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.webhookEvents.WhatsAppData;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.ApiWhatsappService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ApiWhatsappServiceImpl implements ApiWhatsappService {

    private final RestClient restClient;

    public ApiWhatsappServiceImpl(
            @Value("${Phone-Number-ID}") String identificador,
            @Value("${whatsapp.token}") String token,
            @Value("${whatsapp.urlbase}") String urlBase,
            @Value("${whatsapp.version}") String version
            ){

        restClient = RestClient.builder()
                .baseUrl(urlBase + version + "/" + identificador)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    @Override
    public ResponseWhatsapp sendMessage(MessageBody payload) throws JsonProcessingException {
        RequestMessage request = new RequestMessage(
            "whatsapp", 
            "individual", 
            payload.number(),
            "text",
            new RequestMessageText(false, payload.message())
        );

        String response = restClient.post()
                .uri("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);

        ObjectMapper obj = new ObjectMapper();
        return obj.readValue(response, ResponseWhatsapp.class);
    }

    @Override
    public ResponseWhatsapp ResponseMessage(WhatsAppData.WhatsAppMessage message) throws JsonProcessingException {
        RequestMessage request = new RequestMessage(
            "whatsapp", 
            "individual",
            message.entry().get(0).changes().get(0).value().contacts().get(0).wa_id(),
            "text",
            new RequestMessageText(false, "Hola, soy un bot de prueba")

        );

        String response = restClient.post()
                .uri("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);
        
        ObjectMapper obj = new ObjectMapper();
        return obj.readValue(response, ResponseWhatsapp.class);
    }

}
