package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.BackEnd.WhatsappApiCloud.exception.ApiInfoException;
import com.BackEnd.WhatsappApiCloud.exception.CustomJsonServerException;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestMessages;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestMessagesFactory;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestWhatsappAsRead;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.webhookEvents.WhatsAppDataDto;
import com.BackEnd.WhatsappApiCloud.model.dto.openIA.AnswersOpenIADto;
import com.BackEnd.WhatsappApiCloud.model.dto.openIA.QuestionOpenIADto;
import com.BackEnd.WhatsappApiCloud.model.entity.User.UserChatEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.BackEnd.WhatsappApiCloud.repository.UserChatRepository;
import com.BackEnd.WhatsappApiCloud.service.chatSession.ChatSessionService;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.ApiWhatsappService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Paths;

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

    @Value("${service.api.key.openai}")
    private String apiKeyOpenAI;

    @Value("${limit.questions.per.day}")
    private int limitQuestionsPerDay;

    @Value("${hours.to.wait.after.limit}")
    private int hoursToWaitAfterLimit;

    @Value("${strike.limit}")
    private int strikeLimit;

    @Value("${WELCOME_MESSAGE_FILE}")
    private String welcomeMessageFile;

    @Autowired
    private UserChatRepository userChatRepository;

    @Autowired
    private ChatSessionService chatSessionService;


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
            RequestMessages requestBody = RequestMessagesFactory.buildTextMessage(payload.number(), payload.message());
            ResponseWhatsapp response = NewResponseBuilder(requestBody, "/messages");

            if (response != null && response.messages() != null && !response.messages().isEmpty()) {
                chatSessionService.createSessionIfNotExists(payload.number());
            }

            return response;

        } catch (Exception e) {
            logger.error("Error al enviar mensaje: " + e);
            return null;
        }
    }


    // ======================================================
    //   Recibir y enviar respuesta automática
    // ======================================================
    @Override
    public ResponseWhatsapp handleUserMessage(WhatsAppDataDto.WhatsAppMessage message) {
        LocalDateTime timeNow = LocalDateTime.now();

        // Extraer identificadores y datos básicos del mensaje
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
            //! Buscar el usuario o crearlo si no existe
            UserChatEntity user = userChatRepository.findByPhone(waId)
                    .orElseGet(() -> createNewUser(waId, timeNow));

            //! Verificar si el usuario ya está bloqueado
            if (user.isBlock()) {
                return null;
            }

            switch (user.getConversationState()) {
                case "WAITING_FOR_CEDULA":
                    return handleWaitingForCedula(user, messageText, waId, timeNow);
                case "READY":
                    return handleReadyState(user, messageText, waId, timeNow);
                default:
                    user.setConversationState("WAITING_FOR_CEDULA");
                    userChatRepository.save(user);
                    return sendMessage(new MessageBody(waId, "No hemos podido procesar tu solicitud 😕. Por favor, introduce tu número de cédula nuevamente para continuar."));
            }
        } catch (ApiInfoException e) {
            return handleApiInfoException(e, waId);
        } catch (Exception e) {
            logger.error("Error al procesar mensaje de usuario: " + e);
            return sendMessage(new MessageBody(waId, "Ha ocurrido un error inesperado 😕. Por favor, inténtalo nuevamente más tarde."));
        }
    }
    // Método auxiliar para crear un nuevo usuario
    private UserChatEntity createNewUser(String waId, LocalDateTime timeNow) {
        UserChatEntity newUser = new UserChatEntity();
        newUser.setNombres("Anonymus");
        newUser.setPhone(waId);
        newUser.setFirstInteraction(timeNow);
        newUser.setConversationState("WAITING_FOR_CEDULA");
        newUser.setLimitQuestions(3);
        UserChatEntity savedUser = userChatRepository.save(newUser);

        String welcomeMessage = "";
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(welcomeMessageFile));
            welcomeMessage = new String(bytes, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            logger.error("Error al leer el archivo de bienvenida: ", e);
            welcomeMessage = "Error.";
        }

        // sendImageMessageByUrl(waId, "https://static.wikia.nocookie.net/pokemon-unite/images/8/84/Holoatuendo_Gengar_Reportero.png/revision/latest?cb=20220928004647&path-prefix=es");
        // sendVideoMessageByUrl(waId, "https://almacenamiento.ucacue.edu.ec/videos/Dahlia.mp4", "Hola, soy Dahlia, tu asistente virtual. ¿En qué puedo ayudarte?");
        sendStickerMessageByUrl(waId, "https://almacenamiento.ucacue.edu.ec/videos/Dahlia.webp");
        sendMessage(new MessageBody(waId, welcomeMessage));

        return savedUser;
    }
    // Manejo del estado "WAITING_FOR_CEDULA"
    private ResponseWhatsapp handleWaitingForCedula(UserChatEntity user, String messageText, String waId, LocalDateTime timeNow) {
        if (isValidCedula(messageText)) {

            //! Si NO encuentro la cédula dentro de ERP
            UserChatEntity userFromJsonServer = fetchUserFromJsonServer(messageText);
            if (userFromJsonServer == null) {
                user.setLastInteraction(timeNow);
                user.setBlock(true);
                user.setBlockingReason("No pertenece a la Universidad");
                userChatRepository.save(user);
                return sendMessage(new MessageBody(waId, "Actualmente no perteneces a la Universidad Católica de Cuenca ❌. Este servicio es exclusivo para miembros de la universidad."));
            }
            
            //! Si encuentro la cédula dentro de ERP
            else {
                updateUserWithJsonServerData(user, userFromJsonServer, timeNow);
                userChatRepository.save(user);
                return sendMessage(new MessageBody(waId, "¡Hola, " + user.getNombres() + "!👋, ¿Qué consulta te gustaría realizar?"));
            }
        } else {
            //! Si ya se han agotado los intentos
            if (user.getLimitQuestions() <= 0) {
                return null;
            }

            user.setLastInteraction(timeNow);
            user.setLimitQuestions(user.getLimitQuestions() - 1);
            userChatRepository.save(user);
            return sendMessage(new MessageBody(waId, "Verifiquemos tu identidad como miembro de la Universidad. *Ingresa tu número de cédula.* 🔒"));
        }
    }
    // Manejo del estado "READY"
    private ResponseWhatsapp handleReadyState(UserChatEntity user, String messageText, String waId, LocalDateTime timeNow) throws JsonProcessingException {

        //! 1. Verificar si el rol del usuario está denegado
        if (isRoleDenied(user.getRol())) {
            if (user.getLimitQuestions() <= -1) {
                return null;
            }
            user.setLimitQuestions(-1);
            user.setBlockingReason("Rol denegado " + user.getRol());
            userChatRepository.save(user);
            return sendMessage(new MessageBody(waId, "Lo sentimos, esta funcionalidad no está disponible para tu rol de *" + user.getRol() + "* en este momento 🚫."));
        }
            
        //! 2. Verificar strikes
        if (user.getLimitStrike() <= 0) {
            user.setBlock(true);
            user.setBlockingReason("Moderacion");
            userChatRepository.save(user);
            return sendMessage(new MessageBody(waId, "Tu cuenta ha sido bloqueada 🚫. Por favor, comunícate con *soportetic@ucacue.edu.ec* ✉️."));
        }

        // Se reinicia si la fecha de la última interacción es anterior a la fecha actual (se reinicia a las 00:00)
        // Se debera ajusta el 'hours.to.wait.after.limit' a 12 horas
        // if (!user.getLastInteraction().toLocalDate().equals(LocalDate.now())) {
        //     user.setLimitQuestions(limitQuestionsPerDay);
        //     user.setNextResetDate(null);
        //     userChatRepository.save(user);
        // }

        //! 3. Restablece el límite de preguntas diarias si han pasado 24 horas
        if (Duration.between(user.getLastInteraction(), timeNow).toHours() >= 24) {
            user.setLimitQuestions(limitQuestionsPerDay);
            user.setNextResetDate(null);
            userChatRepository.save(user);
        }

        //! 4. Restablece el límite de preguntas y verifica si esta dentro del tiempo de espera segun 'hours.to.wait.after.limit'
        if (user.getNextResetDate() != null) {
            if (timeNow.isAfter(user.getNextResetDate())) {
                user.setLimitQuestions(limitQuestionsPerDay);
                user.setNextResetDate(null);
                userChatRepository.save(user);
            } else {
                Duration remainingTime = Duration.between(timeNow, user.getNextResetDate());
                long hours = remainingTime.toHours();
                long minutes = remainingTime.toMinutes() % 60;
                long seconds = remainingTime.toSeconds() % 60;
                return sendMessage(new MessageBody(waId, String.format("Tu límite de interacciones ha sido alcanzado, tiempo faltante: %02d:%02d:%02d. ⏳", hours, minutes, seconds)));
            }
        }

        //! 5. Si llego al limite de preguntas, restringir por 'hoursToWaitAfterLimit'
        if (user.getLimitQuestions() <= 0) {
            user.setNextResetDate(timeNow.plusHours(hoursToWaitAfterLimit));
            userChatRepository.save(user);
            return sendMessage(new MessageBody(waId, "Tu límite de interacciones ha sido alcanzado, vuelve mañana ⏳."));
        }

        //! 6. Obtener respuesta de IA y Actualizar datos del usuario
        AnswersOpenIADto data = getAnswerIA( new QuestionOpenIADto(messageText, user.getNombres(), waId, user.getRol(),user.getThreadId()));
        UserChatEntity userFromJsonServer = fetchUserFromJsonServer(user.getCedula());
        user.setNombres(userFromJsonServer.getNombres());
        user.setRol(userFromJsonServer.getRol());
        user.setThreadId(data.thread_id());
        user.setLimitQuestions(user.getLimitQuestions() - 1);
        user.setLastInteraction(timeNow);
        user.setEmail(userFromJsonServer.getEmail());
        user.setSede(userFromJsonServer.getSede());
        user.setCarrera(userFromJsonServer.getCarrera());
        user.setValidQuestionCount(user.getValidQuestionCount() + 1);
        userChatRepository.save(user);

        return sendMessage(new MessageBody(waId, data.answer()));
    }
    // Actualiza la información del usuario con los datos del servidor JSON
    private void updateUserWithJsonServerData(UserChatEntity user, UserChatEntity userFromJsonServer, LocalDateTime timeNow) {
        user.setNombres(userFromJsonServer.getNombres());
        user.setCedula(userFromJsonServer.getCedula());
        user.setRol(userFromJsonServer.getRol());
        user.setLimitQuestions(limitQuestionsPerDay);
        user.setLastInteraction(timeNow);
        user.setConversationState("READY");
        user.setLimitStrike(strikeLimit);
        user.setEmail(userFromJsonServer.getEmail());
        user.setSede(userFromJsonServer.getSede());
        user.setCarrera(userFromJsonServer.getCarrera());
        user.setNextResetDate(null);
    }
    // Manejo de ApiInfoException
    private ResponseWhatsapp handleApiInfoException(ApiInfoException e, String waId) {
        userChatRepository.findByPhone(waId).ifPresent(user -> {
            if (e.getModeration() != null) {
                user.setLimitStrike(user.getLimitStrike() - 1);
                userChatRepository.save(user);
            }
        });
        logger.warn("Mensaje informativo recibido: " + e.getInfoMessage());
        return sendMessage(new MessageBody(waId, e.getInfoMessage()));
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
    
            if (users == null || users.isEmpty()) {
                logger.warn("No se encontraron usuarios en el servidor JSON para la cedula " + cedula);
                return null;
            }
    
            // Filtra para obtener el primer usuario que no sea "Estudiante", si no existe, retorna el primer elemento
            // Filtra para excluir los roles restringidos. Si todos son restringidos, toma el primero
            UserChatEntity nonStudentRole = users.stream()
                .filter(user -> !isRoleDenied(user.getRol()))
                .findFirst()
                .orElse(users.get(0));
    
            return nonStudentRole;
        } catch (Exception e) {
            logger.error("Error al obtener datos del usuario desde ERP: " + e.getMessage());
            throw new CustomJsonServerException("Error al obtener datos del usuario desde ERP", e.getCause());
        }
    }    


    // ======================================================
    //   Consulta a IA
    // ======================================================
    private AnswersOpenIADto getAnswerIA(QuestionOpenIADto question) throws JsonMappingException, JsonProcessingException {
        try {
            RestClient openAi = RestClient.builder()
                .baseUrl(baseAIServer)
                .build();
    
            String url = uriAIServer;

            AnswersOpenIADto answer = openAi.post()
                .uri(url)
                .body(question)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKeyOpenAI)
                .retrieve()
                .body(AnswersOpenIADto.class);
    
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
    private ResponseWhatsapp NewResponseBuilder(RequestMessages requestBody, String uri) {
        String response = restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);
    
        ObjectMapper obj = new ObjectMapper();
        try {
            return obj.readValue(response, ResponseWhatsapp.class);
        } catch (JsonProcessingException e) {
            logger.error("Error al procesar JSON: " + e.getMessage());
            throw new RuntimeException("Error processing JSON", e);
        }
    }


    // ======================================================
    //  Cargar imagen a la API de WhatsApp
    // ======================================================
    @Override
    public String uploadMedia(File mediaFile) {
        try {
            // Detectar el tipo MIME de la imagen
            String contentType = Files.probeContentType(mediaFile.toPath());

            if (contentType == null) {
                logger.error("No se pudo detectar el tipo MIME de la imagen.");
                return null;
            }

            // Construir el cuerpo de la petición multipart
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(mediaFile));
            body.add("type", contentType);
            body.add("messaging_product", "whatsapp");

            // Enviar la solicitud usando restClient configurado en el constructor
            String response = restClient.post()
                    .uri("/media")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return response;

        } catch (IOException e) {
            logger.error("❌ Error al leer el archivo de imagen: ", e);
            return null;
        } catch (Exception e) {
            logger.error("❌ Error inesperado al subir la imagen: ", e);
            return null;
        }
    }


    // ======================================================
    //  Enviar una imagen por URL como mensaje
    // ======================================================
    public ResponseWhatsapp sendImageMessageByUrl(String toPhoneNumber, String imageUrl) {
        try {
            RequestMessages mensajeImage = RequestMessagesFactory.buildImageByUrl(toPhoneNumber, imageUrl);

            ResponseWhatsapp respuesta = NewResponseBuilder(mensajeImage, "/messages");
            return respuesta;

        } catch (Exception e) {
            logger.error("❌ Error al enviar la imagen: ", e);
            return null;
        }
    }


    // ======================================================
    //  Enviar una video por URL como mensaje
    // ======================================================
    public ResponseWhatsapp sendVideoMessageByUrl(String toPhoneNumber, String videoUrl, String caption) {
        try {
            RequestMessages mensajeVideo = RequestMessagesFactory.buildVideoByUrl(toPhoneNumber, videoUrl, caption);

            ResponseWhatsapp respuesta = NewResponseBuilder(mensajeVideo, "/messages");
            return respuesta;

        } catch (Exception e) {
            logger.error("❌ Error al enviar el video: ", e);
            return null;
        }
    }


    // ======================================================
    //  Enviar una Sticker statico/animado por URL como mensaje
    // ======================================================
    public ResponseWhatsapp sendStickerMessageByUrl(String toPhoneNumber, String stickerUrl) {
        try {
            RequestMessages mensajeSticker = RequestMessagesFactory.buildStickerByUrl(toPhoneNumber, stickerUrl);

            ResponseWhatsapp respuesta = NewResponseBuilder(mensajeSticker, "/messages");
            return respuesta;

        } catch (Exception e) {
            logger.error("❌ Error al enviar el sticker: ", e);
            return null;
        }
    }

}
