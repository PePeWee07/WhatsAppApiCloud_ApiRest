package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestMessage;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestMessageText;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.webhookEvents.WhatsAppData;
import com.BackEnd.WhatsappApiCloud.model.entity.UserChatEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.BackEnd.WhatsappApiCloud.repository.UserChatRepository;
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
            @Value("${whatsapp.version}") String version) {
        try {
            restClient = RestClient.builder()
                    .baseUrl(urlBase + version + "/" + identificador)
                    .defaultHeader("Authorization", "Bearer " + token)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error initializing RestClient: " + e.getMessage());
        }
    }

    // Constructor de la respuesta
    public ResponseWhatsapp ResponseBuilder(RequestMessage request, String uri) {
        try {
            String response = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            ObjectMapper obj = new ObjectMapper();
            return obj.readValue(response, ResponseWhatsapp.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            System.err.println("Error al construir respuesta: " + e);
            return null;
        }
    }

    // Constructor de la peticion
    public RequestMessage RequestBuilder(String toPhone, String responseType, String responseMessage) {
        try {
            return new RequestMessage(
                    "whatsapp",
                    "individual",
                    toPhone,
                    responseType,
                    new RequestMessageText(false, responseMessage));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al construir RequestMessage: " + e);
            return null;
        }
    }

    // Metodo para enviar mensaje
    @Override
    public ResponseWhatsapp sendMessage(MessageBody payload) {
        try {
            RequestMessage request = RequestBuilder(payload.number(), "text", payload.message());
            return ResponseBuilder(request, "/messages");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al enviar mensaje: " + e);
            return null;
        }
    }

    // Metodo para recibir y responder mensajes automaticamente
    @Override
    public ResponseWhatsapp ResponseMessage(WhatsAppData.WhatsAppMessage message) {
        try {
            String messageType = message.entry().get(0).changes().get(0).value().messages().get(0).type();
            String toPhone = message.entry().get(0).changes().get(0).value().contacts().get(0).wa_id();

            if (!messageType.equals("text")) {
                RequestMessage request = RequestBuilder(toPhone, "text", "Hola, No puedo procesar este tipo de mensaje");
                return ResponseBuilder(request, "/messages");
            }

            RequestMessage request = RequestBuilder(toPhone, messageType, "Hola, soy un bot de prueba");
            return ResponseBuilder(request, "/messages");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al enviar Respuesta: " + e);
            return null;
        }
    }

    //? TEST SAVE USER
    @Autowired
    private UserChatRepository usuarioRepository;
    public UserChatEntity guardarUsuario(UserChatEntity usuario) {
        return usuarioRepository.save(usuario);
    }

}
