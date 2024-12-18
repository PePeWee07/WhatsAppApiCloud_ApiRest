package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.BackEnd.WhatsappApiCloud.exception.ApiInfoException;
import com.BackEnd.WhatsappApiCloud.exception.CustomJsonServerException;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestMessage;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestMessageText;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestWhatsappAsRead;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.webhookEvents.WhatsAppData;
import com.BackEnd.WhatsappApiCloud.model.entity.OpenIA.AnswersOpenIa;
import com.BackEnd.WhatsappApiCloud.model.entity.OpenIA.QuestionOpenIa;
import com.BackEnd.WhatsappApiCloud.model.entity.User.UserChatEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.BackEnd.WhatsappApiCloud.repository.UserChatRepository;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.ApiWhatsappService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ApiWhatsappServiceImpl implements ApiWhatsappService {

    private static final Logger logger = LoggerFactory.getLogger(ApiWhatsappServiceImpl.class);

    private final RestClient restClient;

    @Value("${restricted.roles}")
    private String restrictedRol;

    @Value("${baseurl.jsonserver}")
    private String baseUrlJsonServer;

    @Value("${uri.jsonserver}")
    private String uriJsonServer;

    @Value("${baseurl.aiserver}")
    private String baseAIServer;

    @Value("${uri.aiserver}")
    private String uriAIServer;

    @Value("${limit.questions.per.day}")
    private int limitQuestionsPerDay;

    @Value("${hours.to.wait.after.limit}")
    private int hoursToWaitAfterLimit;

    @Value("${strike.limit}")
    private int strikeLimit;

    @Autowired
    private UserChatRepository userChatRepository;


    // ======================================================
    //   Constructor para inicializar el cliente REST
    // ======================================================
    public ApiWhatsappServiceImpl(
            @Value("${Phone-Number-ID}") String identifier,
            @Value("${whatsapp.token}") String token,
            @Value("${whatsapp.urlbase}") String urlBase,
            @Value("${whatsapp.version}") String version) {
        restClient = RestClient.builder()
                    .baseUrl(urlBase + version + "/" + identifier)
                    .defaultHeader("Authorization", "Bearer " + token)
                    .build();
    }


    // ======================================================
    //   Envio de mensaje a usaurio especifico
    // ======================================================
    @Override
    public ResponseWhatsapp sendMessage(MessageBody payload) {
        try {
            RequestMessage request = RequestBuilder(payload.number(), "text", payload.message());
            return ResponseBuilder(request, "/messages");
        } catch (Exception e) {
            logger.error("Error al enviar mensaje: " + e);
            return null;
        }
    }


    // ======================================================
    //   Recibir y enviar respuesta automatica
    // ======================================================
    @Override
    public ResponseWhatsapp handleUserMessage(WhatsAppData.WhatsAppMessage message) {
        LocalDateTime timeNow = LocalDateTime.now();

        String wamid = message.entry().get(0).changes().get(0).value().messages().get(0).id();
        markAsRead(new RequestWhatsappAsRead("whatsapp", "read", wamid));

        var messageType = message.entry().get(0).changes().get(0).value().messages().get(0).type();
        var waId = message.entry().get(0).changes().get(0).value().contacts().get(0).wa_id();

        var messageOptionalText = message.entry().get(0).changes().get(0).value().messages().get(0).text();
        if (messageOptionalText.isEmpty() || !messageType.equals("text")) {
            logger.warn("El mensaje no contiene texto válido.");
            return null;
        }
        String messageText = messageOptionalText.get().body();

        try {
            UserChatEntity user = userChatRepository.findByPhone(waId)
                .orElseGet(() -> {
                    //! Si no existe el usuario en mi BD, lo creo
                    UserChatEntity newUser = new UserChatEntity();
                    newUser.setNombres("Anonymus");
                    newUser.setPhone(waId);
                    newUser.setFirstInteraction(timeNow);
                    newUser.setConversationState("WAITING_FOR_CEDULA");
                    return userChatRepository.save(newUser);
                });

            switch (user.getConversationState()) {
                case "WAITING_FOR_CEDULA":
                    if (isValidCedula(messageText)) {
                        UserChatEntity userFromJsonServer = fetchUserFromJsonServer(messageText);
    
                        //! Si NO encuentro la cédula dentro de ERP
                        if (userFromJsonServer == null) {
                            user.setLastInteraction(timeNow);
                            userChatRepository.save(user);
                            return sendSimpleResponse(waId, "Actualmente no perteneces a la universidad.");
                        } 
                        //! Si encuentro la cédula dentro de ERP
                        else {
                            user.setNombres(userFromJsonServer.getNombres());
                            user.setCedula(userFromJsonServer.getCedula());
                            user.setRol(userFromJsonServer.getRol());
                            user.setLimitQuestions(limitQuestionsPerDay);
                            user.setLastInteraction(timeNow);
                            user.setConversationState("READY");
                            user.setStrike(strikeLimit);
                            user.setEmail(userFromJsonServer.getEmail());
                            user.setSede(userFromJsonServer.getSede());
                            user.setCarrera(userFromJsonServer.getCarrera());
                            userChatRepository.save(user);
                            return sendSimpleResponse(waId, "Hola " + user.getNombres() + ", bienvenido al Asistente Tecnológico de TICs. ¿En qué puedo ayudarte hoy?");
                        }

                    } else {
                        user.setLastInteraction(timeNow);
                        userChatRepository.save(user);
                        return sendSimpleResponse(waId, "Por favor, introduce tu número de cédula valida para continuar.");
                    }
                case "READY":

                    //! 0. Verificar si el usuario ha sido bloqueado
                    if (user.isBlock()) {
                        return null;
                    }

                    //! 1. Verificar si el rol del usuario está denegado
                    if (isRoleDenied(user.getRol())) {
                        return sendSimpleResponse(waId, "Lo sentimos, esta funcionalidad no está disponible para tu rol de '" + user.getRol() + "' en este momento.");
                    }

                    //! 2. Verificar si el usuario ha sido bloqueado
                    if (user.getStrike() <= 0) {
                        user.setBlock(true);
                        userChatRepository.save(user);
                        return sendSimpleResponse(waId, "Tu cuenta ha sido bloqueada. Por favor, comunícate con soportetic@ucacue.edu.ec.");
                    }

                    //! 3. Verifica si ya pasaron 24 horas para incremetar el límite y restablecer el contador
                    if (!user.getLastInteraction().toLocalDate().isEqual(LocalDate.now())) {
                        user.setLimitQuestions(limitQuestionsPerDay);
                        user.setNextResetDate(null);
                        userChatRepository.save(user);
                    }

                    //! 4. Si ya pasó la fecha de reseteo, restablece el límite
                    if (user.getNextResetDate() != null && timeNow.isAfter(user.getNextResetDate())) {
                        user.setLimitQuestions(limitQuestionsPerDay);
                        user.setNextResetDate(null);
                        userChatRepository.save(user);
                    }

                    //! 5. Si hay una fecha de reseteo definida y aún no ha pasado, no puede realizar la acción
                    if (user.getNextResetDate() != null && timeNow.isBefore(user.getNextResetDate())) {
                        Duration remainingTime = Duration.between(timeNow, user.getNextResetDate());
                        long hours = remainingTime.toHours();
                        long minutes = remainingTime.toMinutes() % 60;
                        long seconds = remainingTime.toSeconds() % 60;
                    
                        return sendSimpleResponse(waId, String.format(
                            "Tu límite de interacciones ha sido alcanzado, tiempo faltante: %02d:%02d:%02d.", 
                            hours, minutes, seconds
                        ));
                    }

                    //! 6. Si el límite de interacciones es 0, bloquear por 24 horas
                    if (user.getLimitQuestions() <= 0) {
                        user.setNextResetDate(timeNow.plusHours(hoursToWaitAfterLimit));
                        userChatRepository.save(user);
                        return sendSimpleResponse(waId, "Tu límite de interacciones ha sido alcanzado, vuelve mañana.");
                    }

                    //! 7. Obtener respuesta de IA y Actualizar datos del usuario
                    AnswersOpenIa data = getAnswerIA(messageText, user.getNombres(), user.getThreadId());
                    UserChatEntity userFromJsonServer = fetchUserFromJsonServer(user.getCedula());
                    user.setNombres(userFromJsonServer.getNombres());
                    user.setRol(userFromJsonServer.getRol());
                    user.setThreadId(data.thread_id());
                    user.setLimitQuestions(user.getLimitQuestions()- 1);
                    user.setLastInteraction(timeNow);
                    user.setEmail(userFromJsonServer.getEmail());
                    user.setSede(userFromJsonServer.getSede());
                    user.setCarrera(userFromJsonServer.getCarrera());
                    userChatRepository.save(user);

                    return sendSimpleResponse(waId, data.answer());
                default:
                    user.setConversationState("WAITING_FOR_CEDULA");
                    userChatRepository.save(user);
                    return sendSimpleResponse(waId, "No hemos podido procesar tu solicitud. Por favor, introduce tu número de cédula nuevamente para continuar.");
            }

        } catch (ApiInfoException e) {
            if (e.getModeration() != null) {
                UserChatEntity user = userChatRepository.findByPhone(waId).orElse(null);
                user.setStrike(user.getStrike() - 1);
                user.setBlockingReason("Moderacion");
                userChatRepository.save(user);
            }
            logger.warn("Mensaje informativo recibido: " + e.getInfoMessage());
            return sendSimpleResponse(waId, e.getInfoMessage());
        } catch (Exception e) {
            logger.error("Error al procesar mensaje de usuario: " + e);
            return sendSimpleResponse(waId, "Ha ocurrido un error inesperado. Por favor, inténtalo nuevamente más tarde.");
        }
    }

    
    // ======================================================
    //   Mensaje leído
    // ======================================================
    public void markAsRead(RequestWhatsappAsRead request) {
        restClient.post().
            uri("/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(String.class);
    }


    // ======================================================
    //   Verificar si el rol del usuario está denegado
    // ======================================================
    public boolean isRoleDenied(String role) {
        return Arrays.asList(restrictedRol.split(",")).contains(role);
    }


    // ======================================================
    //   Validar cédula
    // ======================================================
    private static boolean isValidCedula(String cedula) {
        if (cedula == null || cedula.length() != 10) {
            return false;
        }
    
        try {
            int digitoRegion = Integer.parseInt(cedula.substring(0, 2));

            if (digitoRegion < 1 || digitoRegion > 24) {
                return false;
            }

            int ultimoDigito = Integer.parseInt(cedula.substring(9, 10));

            int pares = Integer.parseInt(cedula.substring(1, 2)) +
                        Integer.parseInt(cedula.substring(3, 4)) +
                        Integer.parseInt(cedula.substring(5, 6)) +
                        Integer.parseInt(cedula.substring(7, 8));

            int impares = sumarImpar(cedula.substring(0, 1)) +
                          sumarImpar(cedula.substring(2, 3)) +
                          sumarImpar(cedula.substring(4, 5)) +
                          sumarImpar(cedula.substring(6, 7)) +
                          sumarImpar(cedula.substring(8, 9));

            int sumaTotal = pares + impares;

            int primerDigitoSuma = Integer.parseInt(String.valueOf(sumaTotal).substring(0, 1));

            int decena = (primerDigitoSuma + 1) * 10;

            int digitoValidador = decena - sumaTotal;

            if (digitoValidador == 10) {
                digitoValidador = 0;
            }

            return digitoValidador == ultimoDigito;
    
        } catch (NumberFormatException e) {
            logger.info(cedula + " no es un número válido.");
            return false;
        }
    }


    // ======================================================
    //   Sumar dígitos impares para validación de cédula
    // ======================================================
    private static int sumarImpar(String numero) {
        int valor = Integer.parseInt(numero) * 2;
        return (valor > 9) ? (valor - 9) : valor;
    }

    
    // ======================================================
    //   Envíos de mensajes simples
    // ======================================================
    private ResponseWhatsapp sendSimpleResponse(String waId, String message) {
        RequestMessage request = RequestBuilder(waId, "text", message);
        return ResponseBuilder(request, "/messages");
    }


    // ======================================================
    //   Simulación de ERP
    // ======================================================
    private UserChatEntity fetchUserFromJsonServer(String cedula) {
        try {
            RestClient localRestClient = RestClient.builder()
                .baseUrl(baseUrlJsonServer)
                .build();

            String url = uriJsonServer + cedula;
            List<UserChatEntity> users = localRestClient.get()
                .uri(url)
                .retrieve()
                .body(new ParameterizedTypeReference<List<UserChatEntity>>() {});

                System.out.println("Users: " + localRestClient.get().uri(url).retrieve().body(String.class)); //! Debug
            
            UserChatEntity nonStudentRole = users.stream()
                .filter(user -> !user.getRol().equalsIgnoreCase("Estudiante"))
                .findFirst().orElse(users.get(0));

            return nonStudentRole;
        } catch (Exception e) {
            logger.error("Error al obtener datos del usuario desde ERP " + e.getMessage());
            throw new CustomJsonServerException("Error al obtener datos del usuario desde ERP", e.getCause());
        }
    }  
    

    // ======================================================
    //   Consulta a IA
    // ======================================================
    private AnswersOpenIa getAnswerIA(String ask, String name, String thread_id) throws JsonMappingException, JsonProcessingException {
        try {
            RestClient openAi = RestClient.builder()
                .baseUrl(baseAIServer)
                .build();
    
            String url = uriAIServer;
            
            QuestionOpenIa question = new QuestionOpenIa(ask, name, thread_id);

            AnswersOpenIa answer = openAi.post()
                .uri(url)
                .body(question)
                .header("Content-Type", "application/json")
                .retrieve()
                .body(AnswersOpenIa.class);
    
            return answer;
    
        }  catch (HttpClientErrorException.BadRequest e) {
            String responseBody = e.getResponseBodyAsString();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            if (rootNode.has("info")) {
                String infoMessage = rootNode.get("info").asText();
                    if (rootNode.has("moderation")) {
                        String moderationValue = rootNode.get("moderation").asText();
                        throw new ApiInfoException(infoMessage, moderationValue);
                    }
                throw new ApiInfoException(infoMessage, null);
            } else {
                logger.error("Bad Request al obtener respuesta de IA: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }  catch (Exception e) {
            logger.error("Error al obtener respuesta de IA: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }


    // ======================================================
    //   Constructor de mensajes de respuesta
    // ======================================================
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
            logger.error("Error al procesar JSON: " + e);
            throw new RuntimeException("Error processing JSON", e);
        }
    }


    // ======================================================
    //   Constructor de mensajes de petición
    // ======================================================
    public RequestMessage RequestBuilder(String toPhone, String responseType, String responseMessage) {
        try {
            return new RequestMessage(
                    "whatsapp",
                    "individual",
                    toPhone,
                    responseType,
                    new RequestMessageText(false, responseMessage));
        } catch (Exception e) {
            logger.error("Error al construir mensaje de petición: " + e);
            return null;
        }
    }
 
}
