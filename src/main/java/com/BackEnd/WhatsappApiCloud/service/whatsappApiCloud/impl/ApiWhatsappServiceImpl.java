package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.BackEnd.WhatsappApiCloud.exception.CustomJsonServerException;
import com.BackEnd.WhatsappApiCloud.exception.CustomOpenIaServerException;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestMessage;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestMessageText;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.webhookEvents.WhatsAppData;
import com.BackEnd.WhatsappApiCloud.model.entity.OpenIA.AnswersOpenIa;
import com.BackEnd.WhatsappApiCloud.model.entity.OpenIA.QuestionOpenIa;
import com.BackEnd.WhatsappApiCloud.model.entity.User.UserChatEntity;
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

    // Metodo para enviar mensaje a un usuario
    @Override
    public ResponseWhatsapp sendMessage(MessageBody payload) {
        try {
            RequestMessage request = RequestBuilder(payload.number(), "text", payload.message());
            return ResponseBuilder(request, "/messages");
        } catch (Exception e) {
            System.err.println("Error al enviar mensaje: " + e);
            return null;
        }
    }

    // Metodo Recibir y enviar respuesta automatica
    @Override
    public ResponseWhatsapp handleUserMessage(WhatsAppData.WhatsAppMessage message) {
        String messageType = message.entry().get(0).changes().get(0).value().messages().get(0).type();
        String waId = message.entry().get(0).changes().get(0).value().contacts().get(0).wa_id();
        String messageText = message.entry().get(0).changes().get(0).value().messages().get(0).text().get().body();

        if (!messageType.equals("text") || messageText == null || messageText.isEmpty()) {
            return sendSimpleResponse(waId, "Lo sentimos, no es posible procesar este tipo de mensaje. Por favor, verifica el formato o el contenido e inténtalo nuevamente.");
        }

        try {
            UserChatEntity user = userChatRepository.findByPhone(waId)
                .orElseGet(() -> {
                    UserChatEntity newUser = new UserChatEntity();
                    newUser.setPhone(waId);
                    newUser.setNombres("Anonymus");
                    newUser.setConversationState("WAITING_FOR_CEDULA");
                    return userChatRepository.save(newUser);
                });

            switch (user.getConversationState()) {
                case "WAITING_FOR_CEDULA":
                    if (isValidCedula(messageText)) {
                        UserChatEntity userFromJsonServer = fetchUserFromJsonServer(messageText);
    
                        //! Si no encuentro la cédula dentro de ERP
                        if (userFromJsonServer == null) {
                            user.setLastInteraction(0);
                            user.setNombres("Usuario");
                            user.setConversationState("READY");
                            user.setRol("Invitado");
                            userChatRepository.save(user);
                            return sendSimpleResponse(waId, "Actualmente estás en modo invitado y no perteneces a la universidad. ¿En qué puedo ayudarte?");
                        }
    
                        //! Si Lo encuentro
                        user.setLastInteraction(0);
                        user.setNombres(userFromJsonServer.getNombres());
                        user.setCarrera(userFromJsonServer.getCarrera());
                        user.setCedula(userFromJsonServer.getCedula());
                        user.setConversationState("READY");
                        user.setRol(userFromJsonServer.getRol());
                        user.setSede(userFromJsonServer.getSede());
                        userChatRepository.save(user);

                        //! Enviar mensaje de bienvenida
                        return sendSimpleResponse(waId, "Hola " + user.getNombres() + ", bienvenido al Asistente Tecnológico de TICs. ¿En qué puedo ayudarte hoy?");

                    } else {
                        return sendSimpleResponse(waId, "Por favor, introduce tu número de cédula para continuar.");
                    }
                case "READY":
                    AnswersOpenIa data = getAnswerIA(messageText, user.getNombres(), user.getThread_id());
                    user.setThread_id(data.thread_id());
                    userChatRepository.save(user);
                    return sendSimpleResponse(waId, data.respuesta());
                default:
                    user.setConversationState("WAITING_FOR_CEDULA");
                    userChatRepository.save(user);
                    return sendSimpleResponse(waId, "No hemos podido procesar tu solicitud. Por favor, introduce tu número de cédula nuevamente para continuar.");
            }

        } catch (Exception e) {
            System.err.println("Error al procesar mensaje de usuario: " + e.getMessage());
            return sendSimpleResponse(waId, "Ha ocurrido un error inesperado. Por favor, inténtalo nuevamente más tarde.");
        }
    }

    // Metodo para validar cedula
    private boolean isValidCedula(String cedula) {
        return cedula != null && cedula.matches("\\d{10}");
    }
    
    // Metodo para enviar mensaje simple
    private ResponseWhatsapp sendSimpleResponse(String waId, String message) {
        RequestMessage request = RequestBuilder(waId, "text", message);
        return ResponseBuilder(request, "/messages");
    }

    // Metodo para obtener usuario desde ERP
    private UserChatEntity fetchUserFromJsonServer(String cedula) {
        try {
            RestClient localRestClient = RestClient.builder()
                .baseUrl("http://localhost:3000")
                .build();

            String url = "/data?cedula=" + cedula;
            List<UserChatEntity> users = localRestClient.get()
                .uri(url)
                .retrieve()
                .body(new ParameterizedTypeReference<List<UserChatEntity>>() {});

            return users.isEmpty() ? null : users.get(0);
        } catch (Exception apiException) {
            System.err.println("Error al obtener datos del usuario desde ERP: " + apiException.getMessage());
            throw new CustomJsonServerException("Error al obtener datos del usuario desde ERP.", apiException);
        }
    }  
    
    // Metodo para obtener respuesta de IA
    private AnswersOpenIa getAnswerIA(String pregunta, String nombre, String thread_id) {
        try {
            RestClient openAi = RestClient.builder()
                .baseUrl("http://127.0.0.1:5000")
                .build();
    
            String url = "/preguntar";
            
            QuestionOpenIa question = new QuestionOpenIa(pregunta, nombre, thread_id);

            AnswersOpenIa answer = openAi.post()
                .uri(url)
                .body(question)
                .header("Content-Type", "application/json")
                .retrieve()
                .body(AnswersOpenIa.class);
    
            return answer;
    
        } catch (Exception e) {
            System.err.println("Error al obtener respuesta de IA: " + e.getMessage());
            throw new CustomOpenIaServerException("Error al obtener respuesta de IA: ", e);
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
            System.err.println("Error al procesar JSON: " + e);
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
 
}
