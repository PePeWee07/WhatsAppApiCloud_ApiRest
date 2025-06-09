package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.BackEnd.WhatsappApiCloud.exception.ApiInfoException;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestMessages;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestMessagesFactory;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestWhatsappAsRead;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.webhookEvents.WhatsAppDataDto;
import com.BackEnd.WhatsappApiCloud.model.dto.erp.ErpUserDto;
import com.BackEnd.WhatsappApiCloud.model.dto.erp.RolUserDto;
import com.BackEnd.WhatsappApiCloud.model.dto.openIA.AnswersOpenIADto;
import com.BackEnd.WhatsappApiCloud.model.dto.openIA.QuestionOpenIADto;
import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.BackEnd.WhatsappApiCloud.repository.UserChatRepository;
import com.BackEnd.WhatsappApiCloud.service.chatSession.ChatSessionService;
import com.BackEnd.WhatsappApiCloud.service.erp.ErpCacheService;
import com.BackEnd.WhatsappApiCloud.service.erp.ErpJsonServerClient;
import com.BackEnd.WhatsappApiCloud.service.openAi.openAiServerClient;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.ApiWhatsappService;
import com.BackEnd.WhatsappApiCloud.util.ConversationState;
import com.fasterxml.jackson.core.JsonProcessingException;
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

    @Value("${limit.questions.per.day}")
    private int limitQuestionsPerDay;

    @Value("${hours.to.wait.after.limit}")
    private int hoursToWaitAfterLimit;

    @Value("${strike.limit}")
    private int strikeLimit;

    @Value("${WELCOME_MESSAGE_FILE}")
    private String welcomeMessageFile;

    @Autowired
    UserChatRepository userChatRepository;
    @Autowired
    ErpJsonServerClient erpJsonServerClient;
    @Autowired
    openAiServerClient openAiServerClient;
    @Autowired
    ChatSessionService chatSessionService;
    @Autowired
    ErpCacheService erpCacheService;

    // ======================================================
    // Constructor para inicializar el cliente REST
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
    // Constructor de mensajes de respuesta
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
    // Envio de mensaje
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
            throw new RuntimeException("Error al enviar mensaje", e);
        }
    }

    
    // ======================================================
    // Mensaje leído
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
    // Crear Usuario
    // ======================================================
    private UserChatEntity createNewUser(String waId, LocalDateTime timeNow) {
        UserChatEntity newUser = new UserChatEntity();
        newUser.setIdentificacion("Anonymus");
        newUser.setWhatsappPhone(waId);
        newUser.setFirstInteraction(timeNow);
        newUser.setConversationState(ConversationState.NEW);
        newUser.setLimitQuestions(5);
        UserChatEntity savedUser = userChatRepository.save(newUser);

        return savedUser;
    }


    // ======================================================
    // Enviar mensaje de bienvenida
    // ======================================================
    private ResponseWhatsapp sendWelcomeMessage(UserChatEntity user, String waId) {
        String welcomeMessage = "";
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(welcomeMessageFile));
            welcomeMessage = new String(bytes, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            logger.error("Error al leer el archivo de bienvenida: ", e);
            welcomeMessage = "Mensaje de bienvenidad no econtrado. Por favor, reporta a soportetic@ucaue.edu.ec.";
        }
        sendStickerMessageByUrl(waId, "https://almacenamiento.ucacue.edu.ec/videos/VA-with-logo-uc-Photoroom-ezgif.com-png-to-webp-converter.webp");
        sendMessage(new MessageBody(waId, welcomeMessage));
        user.setConversationState(ConversationState.ASKED_FOR_CEDULA);
        userChatRepository.save(user);
        return sendMessage(new MessageBody(waId, "Para comenzar, por favor, *ingresa tu número de cédula o identificación* 🔒."));
    }


    // ======================================================
    // Estado "WAITING_FOR_CEDULA"
    // ======================================================
    @Transactional
    private ResponseWhatsapp handleWaitingForCedula(UserChatEntity user, String messageText, String waId, LocalDateTime timeNow) {

        ErpUserDto dto = erpJsonServerClient.getUser(messageText);
        
        //! ========= FALLO DE ERP O IDENTIFICACIÓN NO EXISTE =========
        if (dto == null || dto.getIdentificacion() == null) {
            user.setLastInteraction(timeNow);
            user.setLimitQuestions(user.getLimitQuestions() - 1);

            if (user.getLimitQuestions() <= 0) {
                user.setBlock(true);
                user.setBlockingReason("Identificación no encontrada en ERP");
                userChatRepository.save(user);
                return sendMessage(new MessageBody(waId,
                    "Lo sentimos, no encontramos tu registro tras varios intentos. "
                + "Por seguridad hemos bloqueado tu acceso. "
                + "Si crees que es un error, escribe a soportetic@ucacue.edu.ec"
                ));
            } else {
                userChatRepository.save(user);
                return sendMessage(new MessageBody(waId,
                    "No encontramos tu número de identificación. "
                + "Te quedan " + user.getLimitQuestions() + " intentos. "
                + "Por favor, inténtalo de nuevo."
                ));
            }
        }

        //! ========= SI LO ENCONTRÓ EN ERP =========
        user.setIdentificacion(dto.getIdentificacion());
        user.setLastInteraction(timeNow);
        user.setConversationState(ConversationState.READY);
        user.setLimitQuestions(limitQuestionsPerDay);
        user.setLimitStrike(strikeLimit);
        user.setNextResetDate(null);
        userChatRepository.save(user);

        return sendMessage(new MessageBody(waId,
            "¡Hola 👋, " + dto.getNombres() + " " + dto.getApellidos() + "! ¿En qué puedo ayudarte hoy?"));
    }


    // ======================================================
    // Verificar si el rol del usuario está denegado
    // ======================================================
    private boolean allRolesAreRestricted(List<RolUserDto> rolesUsuarioDto) {
        if (rolesUsuarioDto == null || rolesUsuarioDto.isEmpty()) {
            return true;
        }

        Set<String> restrictedSet = Arrays.stream(restrictedRol.split(","))
                                        .map(String::trim)
                                        .map(String::toUpperCase)
                                        .collect(Collectors.toSet());

        return rolesUsuarioDto.stream()
                            .map(RolUserDto::getTipoRol)
                            .map(String::trim)
                            .map(String::toUpperCase)
                            .allMatch(restrictedSet::contains);
    }


    // ======================================================
    // Estado "READY"
    // ======================================================
    @Transactional
    private ResponseWhatsapp handleReadyState(UserChatEntity user, String messageText, String waId, LocalDateTime timeNow) throws JsonProcessingException {
            
        ErpUserDto userDto = erpCacheService.getCachedUser(user.getIdentificacion());

        if (userDto == null || userDto.getIdentificacion() == null) {
            return sendMessage(new MessageBody(waId, "Hubo un problema al obtner tus datos desde el ERP."));
        }

        //! 1. Verificar si el rol del usuario está denegado
        if (allRolesAreRestricted(userDto.getRolesUsuario())) {
            if (user.getLimitQuestions() <= -1) {
                return null;
            }
            user.setLimitQuestions(-1);
            userChatRepository.save(user);
            return sendMessage(new MessageBody(
                waId,
                "Lo sentimos, pero este asistente virtual aún no está disponible para los siguientes rol(es): *" + restrictedRol + "*."
            ));
        }
            
        //! 2. Verificar strikes
        if (user.getLimitStrike() <= 0) {
            user.setBlock(true);
            user.setBlockingReason("Moderacion");
            userChatRepository.save(user);
            return sendMessage(new MessageBody(waId, "Tu cuenta ha sido bloqueada 🚫. Por favor, comunícate con *soportetic@ucacue.edu.ec* ✉️."));
        }

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

        //! 6. Obtener respuesta de IA
        List<String> userRoles = userDto.getRolesUsuario().stream().map(RolUserDto::getTipoRol).collect(Collectors.toList());

        QuestionOpenIADto question = new QuestionOpenIADto(
            messageText,
            userDto.getNombres() + " " + userDto.getApellidos(),
            waId,
            userRoles,
            user.getThreadId(),
            user.getIdentificacion(),
            userDto.getEmailInstitucional(),
            userDto.getEmailPersonal(),
            userDto.getSexo()
        );

        AnswersOpenIADto data = openAiServerClient.getOpenAiData(question);

        user.setThreadId(data.thread_id());
        user.setLimitQuestions(user.getLimitQuestions() - 1);
        user.setLastInteraction(timeNow);
        user.setValidQuestionCount(user.getValidQuestionCount() + 1);
        userChatRepository.save(user);

        return sendMessage(new MessageBody(waId, data.answer()));
    }

    // ======================================================
    // LLegada de Exepeciones Informativas o Moderación de IA
    // ======================================================
    private ResponseWhatsapp handleApiInfoException(ApiInfoException e, String waId) {
        userChatRepository.findByWhatsappPhone(waId).ifPresent(user -> {
            if (e.getModeration() != null) {
                user.setLimitStrike(user.getLimitStrike() - 1);
                userChatRepository.save(user);
            }
        });
        logger.warn("Mensaje informativo recibido: " + e.getInfoMessage());
        return sendMessage(new MessageBody(waId, e.getInfoMessage()));
    }


    // ======================================================
    // Recibir y enviar respuesta automática
    // ======================================================
    @Override
    public ResponseWhatsapp handleUserMessage(WhatsAppDataDto.WhatsAppMessage message) {
        LocalDateTime timeNow = LocalDateTime.now();

        // Marcar el mensaje como leído
        String wamid = message.entry().get(0).changes().get(0).value().messages().get(0).id();
        markAsRead(new RequestWhatsappAsRead("whatsapp", "read", wamid));
        
        // Extraer datos básicos del mensaje
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
            UserChatEntity user = userChatRepository.findByWhatsappPhone(waId)
                    .orElseGet(() -> createNewUser(waId, timeNow));

            //! Verificar si el usuario ya está bloqueado
            if (user.isBlock()) {
                return null;
            }

            //! Verificar el estado de la conversación
            switch (user.getConversationState()) {

                case NEW: {
                    return sendWelcomeMessage(user, waId);
                }

                case ASKED_FOR_CEDULA: {
                    return handleWaitingForCedula(user, messageText, waId, timeNow);
                }

                case READY: {
                    return handleReadyState(user, messageText, waId, timeNow);
                }

                default: {
                    user.setConversationState(ConversationState.NEW);
                    userChatRepository.save(user);
                    return sendMessage(new MessageBody(waId,
                        "¡Ups! Algo inesperado ocurrió. Reiniciemos. \n"
                    + "Por favor, Escribe un mensaje para comenzar."));
                }
            }
        } catch (ApiInfoException e) {
            return handleApiInfoException(e, waId);
        } catch (Exception e) {
            logger.error("Error al procesar mensaje de usuario: " + e);
            return sendMessage(new MessageBody(waId, "Ha ocurrido un error inesperado 😕. Por favor, inténtalo nuevamente más tarde."));
        }
    }


    // ======================================================
    // Cargar imagen a la API de WhatsApp
    // ======================================================
    @Override
    public String uploadMedia(File mediaFile) {
        try {
            // Detectar el tipo MIME de la imagen
            String contentType = Files.probeContentType(mediaFile.toPath());

            if (contentType == null) {
                logger.error("No se pudo detectar el tipo MIME de la imagen.");
                throw new RuntimeException("No se pudo detectar el tipo MIME de la imagen.");
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
            logger.error("Error al leer el archivo: ", e);
            throw new RuntimeException("Error al leer el archivo: ", e);
        } catch (Exception e) {
            logger.error("Error inesperado al subir el archivo: ", e);
            throw new RuntimeException("Error inesperado al subir el archivo: ", e);
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
            logger.error("Error al enviar la imagen: ", e);
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
            logger.error("Error al enviar el video: ", e);
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
            logger.error("Error al enviar el sticker: ", e);
            return null;
        }
    }

}
