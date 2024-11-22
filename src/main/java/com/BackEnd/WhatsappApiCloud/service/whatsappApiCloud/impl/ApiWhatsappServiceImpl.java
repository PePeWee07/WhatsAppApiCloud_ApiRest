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
    @Autowired
    private UserChatRepository userChatRepository;

    // Constructor para inicializar el cliente REST
    public ApiWhatsappServiceImpl(
            @Value("${Phone-Number-ID}") String identificador,
            @Value("${whatsapp.token}") String token,
            @Value("${whatsapp.urlbase}") String urlBase,
            @Value("${whatsapp.version}") String version) {
        restClient = RestClient.builder()
                    .baseUrl(urlBase + version + "/" + identificador)
                    .defaultHeader("Authorization", "Bearer " + token)
                    .build();
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

    // Parte de ResponseMessage adaptada a tu modelo
    @Override
    public ResponseWhatsapp ResponseMessage(WhatsAppData.WhatsAppMessage message) {
        try {
            String messageType = message.entry().get(0).changes().get(0).value().messages().get(0).type();
            String waId = message.entry().get(0).changes().get(0).value().contacts().get(0).wa_id();

            // Buscar usuario por WA ID (telefono)
            // ? Simular Erp_emulator
            UserChatEntity user = userChatRepository.findByPhone(waId)
            .orElseGet(() -> {
                UserChatEntity newUser = new UserChatEntity();
                newUser.setNombres("Anonymus");
                newUser.setCedula("0000000000");
                newUser.setPhone(waId);
                newUser.setRol("Invitado");
                newUser.setThread_id("xxxxxx");
                newUser.setLastInteraction(System.currentTimeMillis());
                return userChatRepository.save(newUser);  // Guarda inmediatamente el usuario nuevo
            });

            if (!messageType.equals("text")) {
                RequestMessage request = RequestBuilder(waId, "text", "Hola, no puedo procesar este tipo de mensaje.");
                return ResponseBuilder(request, "/messages");
            }

             // Verificar si el usuario es un invitado o no
            if (user.getRol().equals("Invitado")) {
                RequestMessage request = RequestBuilder(waId, "text", "Estás como modo invitado. No perteneces a la universidad.");
                return ResponseBuilder(request, "/messages");
            } else {
                // Enviar datos del usuario a la IA y obtener respuesta
                String respuestaIA = "Respuesta simulada de la IA basada en los datos del usuario.";
                RequestMessage request = RequestBuilder(waId, "text", respuestaIA);

                // Guardar o actualizar usuario
                user.setLastInteraction(System.currentTimeMillis());
                userChatRepository.save(user);

                return ResponseBuilder(request, "/messages");
            }

            // RequestMessage request = RequestBuilder(waId, messageType, "Hola, soy un bot de prueba.");
            // return ResponseBuilder(request, "/messages");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al enviar respuesta: " + e);
            return null;
        }
    }

    // Metodo para construir mensaje de respuesta
    private ResponseWhatsapp ResponseBuilder(RequestMessage request, String uri) {
        String response = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);
        ObjectMapper obj = new ObjectMapper();
        try {
            return obj.readValue(response, ResponseWhatsapp.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing JSON", e);
        }
    }

    // Metodo para construir mensaje de petición
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

    @Override
    public UserChatEntity guardarUsuario(UserChatEntity usuario) {
        return userChatRepository.save(usuario);
    }
}
