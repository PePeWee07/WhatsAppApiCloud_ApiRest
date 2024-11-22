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
import com.BackEnd.WhatsappApiCloud.model.entity.AnswersOpenIa;
import com.BackEnd.WhatsappApiCloud.model.entity.UserChatEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.QuestionOpenIa;
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
    public ResponseWhatsapp handleUserMessage(WhatsAppData.WhatsAppMessage message) {
        try {
            String messageType = message.entry().get(0).changes().get(0).value().messages().get(0).type();
            String waId = message.entry().get(0).changes().get(0).value().contacts().get(0).wa_id();
            String messageText = message.entry().get(0).changes().get(0).value().messages().get(0).text().get().body();

            if (!messageType.equals("text") || messageText == null || messageText.isEmpty()) {
                return sendSimpleResponse(waId, "Hola, no puedo procesar este tipo de mensaje.");
            }

            // Buscar usuario en la base de datos o crearlo como invitado
            UserChatEntity user = userChatRepository.findByPhone(waId)
                .orElseGet(() -> {
                    UserChatEntity newUser = new UserChatEntity();
                    newUser.setPhone(waId);
                    newUser.setNombres("Anonymus");
                    newUser.setConversationState("WAITING_FOR_CEDULA"); // Estado inicial
                    return userChatRepository.save(newUser);
                });

            // Manejar el flujo basado en el estado
            switch (user.getConversationState()) {
                case "WAITING_FOR_CEDULA":
                    if (isValidCedula(messageText)) {
                        UserChatEntity userFromJsonServer = fetchUserFromJsonServer(messageText);
    
                        //! Si no encuentro la cédula dentro de json-server
                        if (userFromJsonServer == null) {
                            user.setLastInteraction(0);
                            user.setNombres("Usuario");
                            user.setConversationState("READY"); // Cambiar estado a READY
                            user.setRol("Invitado");
                            userChatRepository.save(user);
                            return sendSimpleResponse(waId, "Estás como modo invitado. No perteneces a la universidad, pero en que puedo ayudarte?.");
                        }
    
                        //! Si Lo encuentro
                        user.setLastInteraction(0);
                        user.setNombres(userFromJsonServer.getNombres());
                        user.setCarrera(userFromJsonServer.getCarrera());
                        user.setCedula(userFromJsonServer.getCedula());
                        user.setConversationState("READY"); // Cambiar estado a READY
                        user.setRol(userFromJsonServer.getRol());
                        user.setSede(userFromJsonServer.getSede());
                        userChatRepository.save(user);

                        //! Enviar mensaje de bienvenida
                        return sendSimpleResponse(waId, "Hola " + user.getNombres() + ", bienvenido al Chat con OpenIA. que se puede hacer por ti?");

                    } else {
                        return sendSimpleResponse(waId, "Por favor, ingresa tu número de cédula.");
                    }
                case "READY":
                    //! Llamar a la API de IA
                    AnswersOpenIa data = getAnswerIA(messageText, user.getNombres(), user.getThread_id());
                    user.setThread_id(data.thread_id());
                    userChatRepository.save(user);
                    return sendSimpleResponse(waId, data.respuesta());
                default:
                    // En caso de estado desconocido, reiniciar el flujo
                    user.setConversationState("WAITING_FOR_CEDULA");
                    userChatRepository.save(user);
                    return sendSimpleResponse(waId, "No te entiendo. Por favor, ingresa tu número de cédula.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al manejar el mensaje: " + e.getMessage());
            return sendSimpleResponse(null, "Ocurrió un error. Por favor, intenta más tarde.");
        }
    }

    private boolean isValidCedula(String cedula) {
        return cedula != null && cedula.matches("\\d{10}"); // Ejemplo: cédula de 10 dígitos
    }
    
    private ResponseWhatsapp sendSimpleResponse(String waId, String message) {
        RequestMessage request = RequestBuilder(waId, "text", message);
        return ResponseBuilder(request, "/messages");
    }

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
            System.err.println("Error al conectar con json-server: " + apiException.getMessage());
            throw new CustomJsonServerException("Error al obtener datos del usuario desde json-server.", apiException);
        }
    }  
    
    private AnswersOpenIa getAnswerIA(String pregunta, String nombre, String thread_id) {
        try {
            RestClient openAi = RestClient.builder()
                .baseUrl("http://127.0.0.1:5000")
                .build();
    
            String url = "/preguntar";
            
            // Construir el cuerpo de la solicitud
            QuestionOpenIa question = new QuestionOpenIa(pregunta, nombre, thread_id);
    
            // Hacer la llamada POST
            AnswersOpenIa answer = openAi.post()
                .uri(url)
                .body(question) // Enviar el cuerpo en formato JSON
                .header("Content-Type", "application/json") // Asegurar encabezado correcto
                .retrieve()
                .body(AnswersOpenIa.class);
    
            return answer;
    
        } catch (Exception e) {
            System.err.println("Error al obtener respuesta de IA: " + e.getMessage());
            throw new CustomOpenIaServerException("Error al obtener respuesta de IA.", e);
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
