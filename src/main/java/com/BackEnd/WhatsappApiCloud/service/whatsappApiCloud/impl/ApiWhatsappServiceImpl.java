package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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

    
    @Override
    public ResponseWhatsapp ResponseMessage(WhatsAppData.WhatsAppMessage message) {
        try {
            String messageType = message.entry().get(0).changes().get(0).value().messages().get(0).type();
            String waId = message.entry().get(0).changes().get(0).value().contacts().get(0).wa_id();

            // Mensjae de Texto debe traer la cedula
            String messaText = message.entry().get(0).changes().get(0).value().messages().get(0).text().get().body();

            if (!messageType.equals("text")) {
                RequestMessage request = RequestBuilder(waId, "text", "Hola, no puedo procesar este tipo de mensaje.");
                return ResponseBuilder(request, "/messages");
            }

            // Buscar usuario en json-server
            UserChatEntity user;
            try {
                RestClient localRestClient = RestClient.builder()
                    .baseUrl("http://localhost:3000")
                    .build();

                
                String url = "/data?cedula=" + messaText; // Endpoint de json-server
                List<UserChatEntity> users = localRestClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<UserChatEntity>>() {});

                if (users.isEmpty()) {
                    RequestMessage request = RequestBuilder(waId, "text", "Estás como modo invitado. No perteneces a la universidad.");
                    return ResponseBuilder(request, "/messages");
                } else {
                    //? Simular peticion IA
                    // Obtener el primer usuario encontrado
                    user = users.get(0);
                    String respuestaIA = "Hola " + user.getNombres() + ", tu rol es " + user.getRol();
                    RequestMessage request = RequestBuilder(waId, "text", respuestaIA);
                    return ResponseBuilder(request, "/messages");
                }
            } catch (Exception apiException) {
                System.err.println("Error al conectar con json-server: " + apiException.getMessage());
                throw new RuntimeException("Error al obtener datos del usuario desde json-server.");
            }

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
