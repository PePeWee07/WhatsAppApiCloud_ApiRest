package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.impl;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.BackEnd.WhatsappApiCloud.exception.ApiInfoException;
// import com.BackEnd.WhatsappApiCloud.exception.ErpNotFoundException;
import com.BackEnd.WhatsappApiCloud.exception.MediaNotFoundException;
import com.BackEnd.WhatsappApiCloud.exception.ServerClientException;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestMessages;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestMessagesFactory;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestWhatsappAsRead;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.TypingIndicator;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseMediaMetadata;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseMessageTemplate;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsappMessage;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.webhookEvents.WhatsAppDataDto;
import com.BackEnd.WhatsappApiCloud.model.dto.erp.ErpUserDto;
import com.BackEnd.WhatsappApiCloud.model.dto.erp.ErpRolUserDto;
import com.BackEnd.WhatsappApiCloud.model.dto.openIA.AnswersOpenIADto;
import com.BackEnd.WhatsappApiCloud.model.dto.openIA.QuestionOpenIADto;
import com.BackEnd.WhatsappApiCloud.model.entity.user.AttachmentEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageErrorEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessagePricingEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageTemplateEntity;
import com.BackEnd.WhatsappApiCloud.repository.AttachmentRepository;
import com.BackEnd.WhatsappApiCloud.repository.UserChatRepository;
import com.BackEnd.WhatsappApiCloud.repository.message.MessagePricingRepository;
import com.BackEnd.WhatsappApiCloud.repository.message.MessageRepository;
import com.BackEnd.WhatsappApiCloud.repository.message.MessageTemplateRepository;
import com.BackEnd.WhatsappApiCloud.service.erp.ErpCacheService;
import com.BackEnd.WhatsappApiCloud.service.erp.ErpServerClient;
import com.BackEnd.WhatsappApiCloud.service.glpi.GlpiService;
import com.BackEnd.WhatsappApiCloud.service.openAi.AiResponseService;
import com.BackEnd.WhatsappApiCloud.service.openAi.OpenAiServerClient;
import com.BackEnd.WhatsappApiCloud.service.sse.MessageEventStreamService;
import com.BackEnd.WhatsappApiCloud.service.tools.ToolPermissionService;
import com.BackEnd.WhatsappApiCloud.service.tools.ToolUsageService;
import com.BackEnd.WhatsappApiCloud.service.userChat.UserChatSessionService;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.ApiWhatsappService;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.MessageHistoryService;
import com.BackEnd.WhatsappApiCloud.util.MessageMapperHelper;
import com.BackEnd.WhatsappApiCloud.util.enums.AttachmentStatusEnum;
import com.BackEnd.WhatsappApiCloud.util.enums.ConversationStateEnum;
import com.BackEnd.WhatsappApiCloud.util.enums.MessageDirectionEnum;
import com.BackEnd.WhatsappApiCloud.util.enums.MessageSourceEnum;
import com.BackEnd.WhatsappApiCloud.util.enums.MessageTypeEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ApiWhatsappServiceImpl implements ApiWhatsappService {

    private static final Logger logger = LoggerFactory.getLogger(ApiWhatsappServiceImpl.class);
    private static final String MESSAGE_UPDATE_EVENT_TYPE = "message_update";
    private static final String MESSAGE_READ_EVENT_TYPE = "message_read";

    private final RestClient restClient;
    private final RestClient restMediaClient;
    private final ObjectMapper objectMapper;
    private final GlpiService glpiService;
    private final MessageRepository messageRepository;

    @Value("${restricted.roles}")
    private String restrictedRol;

    @Value("${limit.questions.per.day}")
    private int limitQuestionsPerDay;

    @Value("${hours.to.renew.questions:12}")
    private int hoursToRenewQuestions;

    @Value("${hours.to.wait.after.limit}")
    private int hoursToWaitAfterLimit;

    @Value("${strike.limit}")
    private int strikeLimit;

    @Value("${WELCOME_MESSAGE_FILE}")
    private String welcomeMessageFile;

    @Value("${PHONE_NUMBER}")
    private String businessPhoneNumber;

    @Autowired
    private UserChatRepository userChatRepository;
    @Autowired
    private ErpServerClient erpJsonServerClient;
    @Autowired
    private OpenAiServerClient openAiServerClient;
    @Autowired
    private UserChatSessionService chatSessionService;
    @Autowired
    private ErpCacheService erpCacheService;
    @Autowired
    private AiResponseService chatHistoryService;
    @Autowired
    private MessageTemplateRepository templateMsgRepo;
    @Autowired
    private AttachmentRepository attachmentRepository;
    @Autowired
    private MessagePricingRepository messagePricingRepository;
    @Autowired
    private MessageEventStreamService messageEventStreamService;
    @Autowired
    private MessageHistoryService messageHistoryService;
    @Autowired
    private ToolPermissionService toolPermissionService;
    @Autowired
    private ToolUsageService toolUsageService;

    // ================ Constructor para inicializar el cliente REST =====================
    public ApiWhatsappServiceImpl(
        @Value("${Phone-Number-ID}") String identifier,
        @Value("${whatsapp.token}") String token,
        @Value("${whatsapp.urlbase}") String urlBase,
        @Value("${whatsapp.version}") String version,
        ObjectMapper objectMapper,
        GlpiService glpiService,
        MessageRepository messageRepository,
        MessagePricingRepository messagePricingRepository
        ) {

        restClient = RestClient.builder()
                    .baseUrl(urlBase + version + "/" + identifier)
                    .defaultHeader("Authorization", "Bearer " + token)
                    .build();

        restMediaClient = RestClient.builder()
                          .baseUrl(urlBase + version)
                          .defaultHeader("Authorization", "Bearer " + token)
                          .build();

        this.objectMapper = objectMapper;
        this.glpiService = glpiService;
        this.messageRepository = messageRepository;
        this.messagePricingRepository = messagePricingRepository;
    }

    // ================ Constructor de mensajes de respuesta ==========================
    private ResponseWhatsapp NewResponseBuilder(RequestMessages requestBody, String uri) {
        try {
            String response = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            ObjectMapper obj = new ObjectMapper();
            return obj.readValue(response, ResponseWhatsapp.class);

        } catch (RestClientResponseException e) {
            logger.warn("⚠️ Error HTTP al enviar mensaje: Código {}, Cuerpo: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ServerClientException("Error al enviar el mensaje: " + e.getResponseBodyAsString(), e);

        } catch (JsonProcessingException e) {
            logger.error("❌ Error al procesar JSON de respuesta: " + e.getMessage(), e);
            throw new RuntimeException("Error al procesar JSON", e);

        } catch (Exception e) {
            logger.error("❌ Error inesperado al enviar mensaje:", e);
            throw new ServerClientException("Error inesperado al enviar mensaje", e);
        }
    }
    
    // =================== Envío de mensaje ==========================
    public record SendResult(ResponseWhatsapp response, MessageEntity entity) {}

    @Override
    public ResponseWhatsapp sendMessage(MessageBody payload) {
        return sendMessageAndReturnEntity(payload, true).response();
    }

    private SendResult sendMessageAndReturnEntity(MessageBody payload, boolean notifyImmediately) {
        try {
            RequestMessages requestBody = RequestMessagesFactory.buildTextMessage(
                    payload.number(),
                    payload.message(),
                    payload.contextId());

            ResponseWhatsapp response = NewResponseBuilder(requestBody, "/messages");

            if (response == null || response.messages() == null || response.messages().isEmpty()) {
                return new SendResult(response, null);
            }

            chatSessionService.createSessionIfNotExists(payload.number());

            // Auto-pausa: si un agente humano escribe desde el backoffice, dormimos a CatIA.
            // Los mensajes del sistema (BACK_END) y de la IA no activan la pausa.
            if (payload.source() == MessageSourceEnum.BACK_OFFICE) {
                userChatRepository.findByWhatsappPhone(payload.number()).ifPresent(u -> {
                    if (!u.isIaPaused()) {
                        u.setIaPaused(true);
                        userChatRepository.save(u);
                        logger.info("CatIA pausada automáticamente para {} (mensaje desde backoffice).", payload.number());
                    }
                });
            }

            MessageEntity entity = MessageMapperHelper.createSentMessageEntity(payload, response);
            entity = messageRepository.save(entity);
            if (notifyImmediately) {
                notifyMessageUpdate(entity);
            }

            return new SendResult(response, entity);

        } catch (Exception e) {
            logger.error("Error al enviar mensaje: " + e);
            throw new RuntimeException("Error al enviar mensaje", e);
        }
    }

    // ================ Mensaje leído ===========================
    public void markAsRead(RequestWhatsappAsRead request) {
        restClient.post().
            uri("/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(String.class);
    }

    // =================== Crear Usuario ============================
    private UserChatEntity createNewUser(String waId, LocalDateTime timeNow) {
        UserChatEntity newUser = new UserChatEntity();
        newUser.setIdentificacion("Anonymus");
        newUser.setWhatsappPhone(waId);
        newUser.setFirstInteraction(timeNow);
        newUser.setLastInteraction(timeNow);
        newUser.setConversationState(ConversationStateEnum.NEW);
        newUser.setLimitQuestions(5);
        UserChatEntity savedUser = userChatRepository.save(newUser);

        return savedUser;
    }

    // =============== Enviar mensaje de bienvenida ====================
    private ResponseWhatsapp sendWelcomeMessage(UserChatEntity user, String waId) {
        String welcomeMessage = "";
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(welcomeMessageFile));
            welcomeMessage = new String(bytes, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            logger.error("Error al leer el archivo de bienvenida: ", e);
            welcomeMessage = "Mensaje de bienvenida no econtrado. Por favor, reporta a soportetic@ucaue.edu.ec.";
        }
        sendStickerMessageByUrl(new MessageBody(
            waId,
            null,
            "System",
            MessageSourceEnum.BACK_END,
            businessPhoneNumber,
            MessageTypeEnum.STICKER,
            null), 
            "https://ia-sp-backoffice.ucatolica.cue.ec/uploads/catia.webp"
        );
        sendMessage(new MessageBody(
            waId,
            welcomeMessage,
            "System",
            MessageSourceEnum.BACK_END,
            businessPhoneNumber,
            MessageTypeEnum.TEXT,
            null)
        );
        user.setConversationState(ConversationStateEnum.ASKED_FOR_CEDULA);
        userChatRepository.save(user);
        return sendMessage(new MessageBody(
            waId,
            "Para comenzar, por favor, *ingresa tu número de cédula o identificación* 🔒.",
            "System",
            MessageSourceEnum.BACK_END, 
            businessPhoneNumber, 
            MessageTypeEnum.TEXT, 
            null)
        );
    }

    // ============== Estado "WAITING_FOR_CEDULA" ====================
    @Transactional
    private ResponseWhatsapp handleWaitingForCedula(UserChatEntity user, String messageText, String waId, LocalDateTime timeNow) {
        if (user.getLimitQuestions() <= 0){
                return null;
        }
        ErpUserDto dto;
        try {
            dto = erpJsonServerClient.getUser(messageText);
        } catch (ServerClientException e) {
            user.setLastInteraction(timeNow);
            user.setLimitQuestions(user.getLimitQuestions() - 1);
            userChatRepository.save(user);
            if(user.getLimitQuestions() == 0 ){
                user.setBlock(true);
                user.setBlockingReason("Demasiados intentos fallidos");
                userChatRepository.save(user);
                return sendMessage(new MessageBody(
                    waId, "Demasiados intentos fallidos. Hemos bloqueado tu acceso por seguridad. Por favor, comunícate con soportetic@ucacue.edu.ec",
                        "System", MessageSourceEnum.BACK_END, businessPhoneNumber,  MessageTypeEnum.TEXT, null
                ));
            }
            return sendMessage(new MessageBody(
                waId,
                "No encontramos tu número de identificación en nuestro sistema. " +
                "Te quedan " + user.getLimitQuestions() + " intentos.",
                    "System", MessageSourceEnum.BACK_END, businessPhoneNumber,  MessageTypeEnum.TEXT, null
            ));
        }

        // Encontró el usuario en el ERP
        user.setIdentificacion(dto.getIdentificacion());
        user.setLastInteraction(timeNow);
        user.setConversationState(ConversationStateEnum.READY);
        user.setLimitQuestions(limitQuestionsPerDay);
        user.setLimitStrike(strikeLimit);
        user.setNextResetDate(null);
        userChatRepository.save(user);

        return sendMessage(new MessageBody(
            waId,
            "¡Hola 😊, " + dto.getNombres() + " " + dto.getApellidos() + "! ¿En qué puedo ayudarte hoy?", "System",
                MessageSourceEnum.BACK_END, businessPhoneNumber,  MessageTypeEnum.TEXT, null
        ));
    }

    // ================= Verificar si el rol del usuario está denegado =================
    private boolean allRolesAreRestricted(List<ErpRolUserDto> rolesUsuarioDto) {
        if (rolesUsuarioDto == null || rolesUsuarioDto.isEmpty()) {
            return true;
        }

        Set<String> restrictedSet = Arrays.stream(restrictedRol.split(","))
                                        .map(String::trim)
                                        .map(String::toUpperCase)
                                        .collect(Collectors.toSet());

        return rolesUsuarioDto.stream()
                            .map(ErpRolUserDto::getTipoRol)
                            .map(String::trim)
                            .map(String::toUpperCase)
                            .allMatch(restrictedSet::contains);
    }

    // =========== Save Attachment Document ==============
    private AttachmentEntity saveMediaAttachment(
            String waId,
            WhatsAppDataDto.Message msg,
            String type,
            String attachmentId,
            String mimeType,
            String caption,
            ConversationStateEnum stateAfterSave
        ) {
        AttachmentEntity att = new AttachmentEntity();
        att.setWhatsappPhone(waId);
        att.setTimestamp(Instant.ofEpochSecond(Long.parseLong(msg.timestamp())));
        att.setType(type);
        att.setAttachmentID(attachmentId);
        att.setMimeType(mimeType);
        att.setCaption(caption);

        att.setConversationState(stateAfterSave);
        att.setAttachmentStatus(AttachmentStatusEnum.UNUSED);

        return attachmentRepository.save(att);
    }

    // ================ Estado "READY" =========================
    @Transactional
    private ResponseWhatsapp handleReadyState(UserChatEntity user, String messageText, String waId, LocalDateTime timeNow) throws JsonProcessingException {
            
        ErpUserDto userDto = erpCacheService.getCachedUser(user.getIdentificacion());

        if (userDto == null || userDto.getIdentificacion() == null) {
            return sendMessage(new MessageBody(waId, "Hubo un problema al obtner tus datos desde el ERP.", "System",
                    MessageSourceEnum.BACK_END, businessPhoneNumber,  MessageTypeEnum.TEXT, null));
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
                "Lo sentimos, pero este asistente virtual aún no está disponible para los siguientes rol(es): *" + restrictedRol + "*.",
                    "System", MessageSourceEnum.BACK_END, businessPhoneNumber,  MessageTypeEnum.TEXT, null
            ));
        }
            
        //! 2. Verificar strikes
        if (user.getLimitStrike() <= 0) {
            user.setBlock(true);
            user.setBlockingReason("Moderacion");
            userChatRepository.save(user);
            return sendMessage(new MessageBody(waId, "Tu cuenta ha sido bloqueada 🚫. Por favor, comunícate con *soportetic@ucacue.edu.ec* ✉️.",
                    "System", MessageSourceEnum.BACK_END, businessPhoneNumber,  MessageTypeEnum.TEXT, null));
        }

        //! 3. Restablece el límite de preguntas si ha pasado el periodo de renovación (default 12h)
        if (Duration.between(user.getLastInteraction(), timeNow).toHours() >= hoursToRenewQuestions) {
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
                return sendMessage(new MessageBody(waId, String.format("Tu límite de interacciones ha sido alcanzado, tiempo faltante: %02d:%02d:%02d. ⏳", hours, minutes, seconds),
                        "System", MessageSourceEnum.BACK_END, businessPhoneNumber,  MessageTypeEnum.TEXT, null));
            }
        }

        //! 5. Si llego al limite de preguntas, restringir por 'hoursToWaitAfterLimit'
        if (user.getLimitQuestions() <= 0) {
            user.setNextResetDate(timeNow.plusHours(hoursToWaitAfterLimit));
            userChatRepository.save(user);
            return sendMessage(new MessageBody(waId, "Tu límite de interacciones ha sido alcanzado, vuelve mañana ⏳.",
                    "System", MessageSourceEnum.BACK_END, businessPhoneNumber,  MessageTypeEnum.TEXT, null));
        }

        //! 6. Obtener respuesta de IA
        List<String> userRoles = userDto.getRolesUsuario().stream().map(ErpRolUserDto::getTipoRol).collect(Collectors.toList());

        Map<String, List<String>> toolPerms = toolPermissionService.getPermissionsMap();
        Map<String, Integer> toolCooldowns = toolPermissionService.getCooldownMap();
        Map<String, Integer> toolCooldownRemaining = toolUsageService.getRemainingMap(waId, toolCooldowns);

        QuestionOpenIADto question = new QuestionOpenIADto(
            messageText,
            userDto.getNombres() + " " + userDto.getApellidos(),
            waId,
            userRoles,
            user.getPreviousResponseId(),
            user.getIdentificacion(),
            userDto.getEmailInstitucional(),
            userDto.getEmailPersonal(),
            userDto.getSexo(),
            toolPerms,
            toolCooldowns,
            toolCooldownRemaining
        );

        AnswersOpenIADto data = openAiServerClient.getOpenAiData(question);

        // Registra (last_executed_at = ahora) las tools que Gpt-Tics ejecutó, para el cooldown.
        toolUsageService.recordExecutions(waId, data.executedTools());

        MessageBody outPayload = new MessageBody(
                waId,
                data.answer(),
                "CatIA",
                MessageSourceEnum.IA,
                businessPhoneNumber,
                MessageTypeEnum.TEXT,
                null);

        SendResult sent = sendMessageAndReturnEntity(outPayload, false);

        if (sent.entity() != null) {
            chatHistoryService.saveAiResponses(data, sent.entity());
            notifyMessageUpdate(sent.entity());
        } else {
            logger.warn("No se pudo guardar historial IA: no se guardó MessageEntity del mensaje final.");
        }

        user = userChatRepository.findByWhatsappPhone(waId).orElse(user);
        user.setPreviousResponseId(data.previousResponseId());
        user.setLimitQuestions(user.getLimitQuestions() - 1);
        user.setLastInteraction(timeNow);
        user.setValidQuestionCount(user.getValidQuestionCount() + 1);
        userChatRepository.save(user);

        return sent.response();
    }

    // ================ Envio de exepeciones Informativas o Moderación de IA ===================
    private ResponseWhatsapp handleApiInfoException(ApiInfoException e, String waId) {
        userChatRepository.findByWhatsappPhone(waId).ifPresent(user -> {
            if (e.getModeration() != null) {
                user.setLimitStrike(user.getLimitStrike() - 1);
                userChatRepository.save(user);
            }
        });
        logger.warn("Mensaje informativo recibido: " + e.getInfoMessage());
        return sendMessage(new MessageBody(waId, e.getInfoMessage(), "CatIA", 
                MessageSourceEnum.IA, businessPhoneNumber,  MessageTypeEnum.TEXT, null));
    }

    private Optional<WhatsAppDataDto.Value> getFirstChangeValue(WhatsAppDataDto.WhatsAppMessage message) {
        if (message == null || message.entry() == null || message.entry().isEmpty()) {
            return Optional.empty();
        }

        var entry = message.entry().get(0);
        if (entry == null || entry.changes() == null || entry.changes().isEmpty()) {
            return Optional.empty();
        }

        var change = entry.changes().get(0);
        if (change == null || change.value() == null) {
            return Optional.empty();
        }

        return Optional.of(change.value());
    }

    private boolean isNullOrEmpty(List<?> values) {
        return values == null || values.isEmpty();
    }

    private void logWebhookErrors(List<WhatsAppDataDto.WhatsAppError> errors) {
        if (isNullOrEmpty(errors)) {
            return;
        }

        for (var error : errors) {
            if (error == null) {
                continue;
            }

            String details = error.error_data() != null ? error.error_data().details() : null;
            logger.warn(
                    "Webhook WhatsApp con error general. code={}, title={}, message={}, details={}, href={}",
                    error.code(),
                    error.title(),
                    error.message(),
                    details,
                    error.href());
        }
    }

    private void logFailedStatusError(WhatsAppDataDto.Status status) {
        if (status == null || isNullOrEmpty(status.errors())) {
            logger.warn(
                    "Estado failed recibido para wamid={}, recipient_id={}, sin detalle de error.",
                    status != null ? status.id() : null,
                    status != null ? status.recipient_id() : null);
            return;
        }

        for (var error : status.errors()) {
            if (error == null) {
                continue;
            }

            String details = error.error_data() != null ? error.error_data().details() : null;
            logger.warn(
                    "Estado failed de WhatsApp. wamid={}, recipient_id={}, code={}, title={}, message={}, details={}",
                    status.id(),
                    status.recipient_id(),
                    error.code(),
                    error.title(),
                    error.message(),
                    details);
        }
    }

    private Instant parseWhatsappTimestamp(String timestamp) {
        try {
            return Instant.ofEpochSecond(Long.parseLong(timestamp));
        } catch (Exception e) {
            logger.warn("Timestamp de WhatsApp inválido: {}", timestamp);
            return Instant.now();
        }
    }

    private void attachFirstStatusError(MessageEntity msg, WhatsAppDataDto.Status status) {
        if (msg == null || status == null || isNullOrEmpty(status.errors())) {
            return;
        }

        var err = status.errors().get(0);
        if (err == null) {
            return;
        }

        MessageErrorEntity entityError = Optional.ofNullable(msg.getMessageErrorEntity())
                .orElseGet(MessageErrorEntity::new);
        entityError.setErrorCode(err.code());
        entityError.setErrorTitle(err.title());
        entityError.setErrorMessage(err.message());
        entityError.setErrorDetails(err.error_data() != null ? err.error_data().details() : null);
        entityError.setMessage(msg);
        msg.setMessageErrorEntity(entityError);
    }

    private void notifyMessageUpdate(MessageEntity message) {
        if (message == null || message.getId() == null) {
            return;
        }

        String phone = message.getConversationUserPhone();
        if (phone == null || phone.isBlank() || !messageEventStreamService.hasSubscribers(phone)) {
            return;
        }

        try {
            MessageDto dto = messageHistoryService.getMessageDetailsById(message.getId());
            messageEventStreamService.notifyUpdate(phone, MESSAGE_UPDATE_EVENT_TYPE, dto);
        } catch (Exception ex) {
            logger.warn("No se pudo notificar actualización SSE para messageId={} phone={}",
                    message.getId(),
                    phone,
                    ex);
        }
    }

    private void notifyMessageUpdateById(Long messageId) {
        if (messageId == null) {
            return;
        }

        try {
            MessageDto dto = messageHistoryService.getMessageDetailsById(messageId);
            String phone = dto.getConversationUserPhone();
            if (phone == null || phone.isBlank() || !messageEventStreamService.hasSubscribers(phone)) {
                return;
            }
            messageEventStreamService.notifyUpdate(phone, MESSAGE_UPDATE_EVENT_TYPE, dto);
        } catch (Exception ex) {
            logger.warn("No se pudo notificar actualización SSE para messageId={}", messageId, ex);
        }
    }

    private void notifyOutboundMessageRead(MessageEntity message) {
        if (message == null || message.getId() == null) {
            return;
        }

        if (message.getDirection() != MessageDirectionEnum.OUTBOUND) {
            return;
        }

        String phone = message.getConversationUserPhone();
        if (phone == null || phone.isBlank() || !messageEventStreamService.hasSubscribers(phone)) {
            return;
        }

        try {
            MessageDto dto = messageHistoryService.getMessageDetailsById(message.getId());
            messageEventStreamService.notifyStatusUpdate(phone, MESSAGE_READ_EVENT_TYPE, "read", dto);
        } catch (Exception ex) {
            logger.warn("No se pudo notificar lectura SSE para messageId={} phone={}",
                    message.getId(),
                    phone,
                    ex);
        }
    }

    // =================== Recibir y enviar respuesta automática ======================
    @Override
    public ResponseWhatsapp handleUserMessage(WhatsAppDataDto.WhatsAppMessage message) {
        LocalDateTime timeNow = LocalDateTime.now();
        var changeValueOptional = getFirstChangeValue(message);

        if (changeValueOptional.isEmpty()) {
            logger.warn("Webhook WhatsApp recibido sin entry/changes/value válido.");
            return null;
        }

        var changeValue = changeValueOptional.get();

        if (!isNullOrEmpty(changeValue.errors())) {
            logWebhookErrors(changeValue.errors());
            return null;
        }

        if (!isNullOrEmpty(changeValue.statuses())) {
            handleMessageStatus(message);
            return null;
        }

        if (!isNullOrEmpty(changeValue.messages())) {
            var firstMessage = changeValue.messages().get(0);
            if (firstMessage == null) {
                logger.warn("Webhook WhatsApp con messages[] pero el primer mensaje es null.");
                return null;
            }
            String messageType = firstMessage.type();
            String wamid = firstMessage.id();
            String waId = !isNullOrEmpty(changeValue.contacts())
                    ? changeValue.contacts().get(0).wa_id()
                    : firstMessage.from();
            var messageOptionalText = Optional.ofNullable(firstMessage.text()).orElse(Optional.empty());

            if (waId == null || waId.isBlank()) {
                logger.warn("Webhook WhatsApp con messages[] pero sin wa_id/from válido.");
                return null;
            }

            UserChatEntity user = userChatRepository.findByWhatsappPhone(waId)
                    .orElseGet(() -> createNewUser(waId, timeNow));

            if (user.isBlock()) {
                return null;
            }

            if ("interactive".equals(messageType)) {
                var msg = firstMessage;
                var ctx = msg.context();
                Object rawInteractive = msg.interactive();
                String ts = msg.timestamp();

                LocalDateTime answeredAt = LocalDateTime.ofInstant(
                        parseWhatsappTimestamp(ts),
                        ZoneId.systemDefault());

                if (ctx != null && rawInteractive instanceof Map<?, ?> interactiveMap) {
                    Object nfmObj = interactiveMap.get("nfm_reply");
                    if (nfmObj instanceof Map<?, ?> nfmMap) {

                        String parentWamid = ctx.id();
                        String answerJson = (String) nfmMap.get("response_json");

                        templateMsgRepo.findByMessageWamid(parentWamid).ifPresent(template -> {
                            template.setAnswer(answerJson);
                            template.setAnsweredAt(answeredAt);
                            templateMsgRepo.save(template);
                            messageRepository.findByWamid(parentWamid)
                                    .map(MessageEntity::getId)
                                    .ifPresent(this::notifyMessageUpdateById);
                        });
                    }
                }
                return null;
            }

            // Guardar mensaje entrante
            for (var msg : changeValue.messages()) {
                MessageEntity entity = MessageMapperHelper.fromWebhookMessage(
                    changeValue,
                    msg,
                    MessageDirectionEnum.INBOUND,
                    MessageSourceEnum.USER
                );
                MessageEntity saved = messageRepository.save(entity);
                notifyMessageUpdate(saved);

            }

            // Takeover humano
            if (user.isIaPaused()) {
                logger.info("CatIA en pausa (takeover humano) para {}. Mensaje guardado sin autorespuesta.", waId);
                return null;
            }

            markAsRead(new RequestWhatsappAsRead("whatsapp", "read", wamid, new TypingIndicator("text")));

            // Manejar estados de conversación
            try {
                switch (user.getConversationState()) {
                    case NEW: {
                        return sendWelcomeMessage(user, waId);
                    }
    
                    case ASKED_FOR_CEDULA: {
                        if (messageOptionalText.isEmpty() || !messageType.equals("text")) {
                            return null;
                        }
                        String messageText = messageOptionalText.get().body();
                        return handleWaitingForCedula(user, messageText, waId, timeNow);
                    }
    
                    case READY: {

                        if (messageType.equals("image") || messageType.equals("document") 
                                || messageType.equals("audio") || messageType.equals("video")) {

                            if (user.getLimitQuestions() <= 0) {
                                return null;
                            }
                            
                            return sendMessage(
                                new MessageBody(
                                    waId, 
                                    """
                                    > ⚠️ *Carga de archivos cerrada*

                                    ```Detecté un archivo, pero mi sistema de recepción no está activo.```

                                    🎫 ```¿Es para un soporte?```
                                    Por favor, inicia un ticket de soporte para poder recibir tu información.
                                    """,
                                    "System",
                                    MessageSourceEnum.BACK_END,
                                    businessPhoneNumber, 
                                    MessageTypeEnum.TEXT,
                                    wamid)
                            );
                        }

                        if (messageType.equals("text")) {
                            if (messageOptionalText.isEmpty()) {
                                return null;
                            }

                            String messageText = messageOptionalText.get().body();
                            return handleReadyState(user, messageText, waId, timeNow);
                        }

                        return null;
                    }
    
                    case WAITING_ATTACHMENTS: {
                        boolean expired = user.getAttachStartedAt() == null || user.getAttachTtlMinutes() == null || Instant.now().isAfter(
                            user.getAttachStartedAt().plus(Duration.ofMinutes(user.getAttachTtlMinutes()))
                        );
    
                        if (expired) {
                            // Si expiró, cerramos la sesión de adjuntos y volvemos a READY
                            user.setConversationState(ConversationStateEnum.READY);
                            user.setAttachStartedAt(null);
                            user.setAttachTtlMinutes(null);
                            userChatRepository.save(user);
    
                            return sendMessage(new MessageBody(
                                waId,
                                "⚠️ La sesión para adjuntar expiró. ⚠️ ", "System", 
                                    MessageSourceEnum.BACK_END, businessPhoneNumber,
                                    MessageTypeEnum.TEXT, null
                            ));
                        }

                        var msg = firstMessage;
                        switch (messageType) {
                            case "text" -> {
                                String messageText = messageOptionalText.get().body();
                                user.setConversationState(ConversationStateEnum.READY);
                                userChatRepository.save(user);
                                return handleReadyState(user, messageText, waId, timeNow);
                            }
                            case "image" -> {
                                var img = msg.image().get();

                                saveMediaAttachment(
                                        waId,
                                        msg,
                                        "image",
                                        img.id(),
                                        img.mime_type(),
                                        img.caption(),
                                        ConversationStateEnum.WAITING_ATTACHMENTS);

                                return sendMessage(new MessageBody(
                                        waId, "🖼️ Imagen recibida. Sube más o dime si deseas continuar.", "System",
                                        MessageSourceEnum.BACK_END, businessPhoneNumber, MessageTypeEnum.TEXT, null));
                            }
                            case "document" -> {
                                var doc = msg.document().get();

                                saveMediaAttachment(
                                        waId,
                                        msg,
                                        "document",
                                        doc.id(),
                                        doc.mime_type(),
                                        doc.caption(),
                                        ConversationStateEnum.WAITING_ATTACHMENTS);

                                return sendMessage(new MessageBody(
                                        waId, "📎 Documento recibido. Sube más o dime si deseas continuar.", "System",
                                        MessageSourceEnum.BACK_END, businessPhoneNumber, MessageTypeEnum.TEXT, null));
                            }
                            default -> {
                                return null;
                            }
                        }
                    }
    
                    case WAITING_ATTACHMENTS_FOR_TICKET_EXISTING: {
                        boolean expired = user.getAttachStartedAt() == null || user.getAttachTtlMinutes() == null || Instant.now().isAfter(user.getAttachStartedAt().plus(Duration.ofMinutes(user.getAttachTtlMinutes())));
                        if (expired) {
                            user.setConversationState(ConversationStateEnum.READY);
                            user.setAttachTargetTicketId(null);
                            user.setAttachStartedAt(null);
                            user.setAttachTtlMinutes(null);
                            userChatRepository.save(user);
                            return sendMessage(new MessageBody(waId, "⚠️ La sesión para adjuntar expiró. ⚠️", "System",
                                    MessageSourceEnum.BACK_END, businessPhoneNumber,  MessageTypeEnum.TEXT, null));
                        }

                        var msg = firstMessage;
                        switch (messageType) {
                            case "text" -> {
                                String messageText = messageOptionalText.get().body();
                                try {
                                    glpiService.attachRecentWhatsappMediaToTicket(waId, 
                                        user.getAttachTargetTicketId(), user.getAttachTtlMinutes());
                                } catch (ServerClientException sce) {
                                    logger.warn("Adjuntado fallo: {}", sce.getMessage());
                                    sendMessage(new MessageBody(waId, "⚠️🚨 " + sce.getMessage() + " 🚨⚠️", "System",
                                            MessageSourceEnum.BACK_END, businessPhoneNumber, MessageTypeEnum.TEXT,
                                            null));
                                } finally {
                                    user.setConversationState(ConversationStateEnum.READY);
                                    user.setAttachTargetTicketId(null);
                                    user.setAttachStartedAt(null);
                                    user.setAttachTtlMinutes(null);
                                    userChatRepository.save(user);
                                }
                                return handleReadyState(user, messageText, waId, timeNow);
                            }
                            case "image" -> {
                                var img = msg.image().get();

                                saveMediaAttachment(
                                        waId,
                                        msg,
                                        "image",
                                        img.id(),
                                        img.mime_type(),
                                        img.caption(),
                                        ConversationStateEnum.WAITING_ATTACHMENTS_FOR_TICKET_EXISTING);

                                return sendMessage(new MessageBody(
                                        waId, "🖼️ Imagen recibida. Sube más o dime si deseas continuar.", "System",
                                        MessageSourceEnum.BACK_END, businessPhoneNumber, MessageTypeEnum.TEXT, null));
                            }
                            case "document" -> {
                                var doc = msg.document().get();

                                saveMediaAttachment(
                                        waId,
                                        msg,
                                        "document",
                                        doc.id(),
                                        doc.mime_type(),
                                        doc.caption(),
                                        ConversationStateEnum.WAITING_ATTACHMENTS_FOR_TICKET_EXISTING);

                                return sendMessage(new MessageBody(
                                        waId, "📎 Documento recibido. Sube más o dime si deseas continuar.", "System",
                                        MessageSourceEnum.BACK_END, businessPhoneNumber, MessageTypeEnum.TEXT, null));
                            }
                            default -> {
                                return null;
                            }
                        }
                    }
                    default: {
                        user.setConversationState(ConversationStateEnum.NEW);
                        userChatRepository.save(user);
                        return sendMessage(new MessageBody(
                            waId,
                            "¡Ups! 🫢 Algo inesperado ocurrió. Reiniciemos. \n" + "Por favor, Escribe un mensaje para comenzar.",
                            "System",
                            MessageSourceEnum.BACK_END,
                            businessPhoneNumber,
                            MessageTypeEnum.TEXT,
                            null
                        ));
                    }
                }
            } catch (ApiInfoException e) {
                return handleApiInfoException(e, waId);
            } catch (Exception e) {
                logger.error("Error al procesar mensaje de usuario: ", e);
                return sendMessage(new MessageBody(
                    waId,
                    "Ha ocurrido un error inesperado 😟. Por favor, inténtalo nuevamente más tarde.",
                    "System",
                    MessageSourceEnum.BACK_END, 
                    businessPhoneNumber, 
                    MessageTypeEnum.TEXT,
                    null
                ));
            }
        }

        logger.warn("⚠️ Mensaje recibido sin contenido válido.");
        return null;
    }

    // =================== Recivir estados de mensajes ======================
    @Override
    public void handleMessageStatus(WhatsAppDataDto.WhatsAppMessage status) {
        var changeValueOptional = getFirstChangeValue(status);

        if (changeValueOptional.isEmpty()) {
            logger.warn("Webhook de estado sin entry/changes/value válido.");
            return;
        }

        var changeValue = changeValueOptional.get();

        if (isNullOrEmpty(changeValue.statuses())) {
            logger.warn("⚠️ Webhook de estado sin contenido válido.");
            return;
        }

        for (var s : changeValue.statuses()) {
            if (s == null) {
                continue;
            }

            String waId = s.recipient_id();
            String messageId = s.id();
            String state = s.status();
            String timestamp = s.timestamp();

            logger.info("Estado de mensaje WhatsApp recibido. wamid={}, recipient_id={}, status={}", messageId, waId, state);

            if (waId == null || waId.isBlank() || messageId == null || messageId.isBlank() || state == null || state.isBlank()) {
                logger.warn("Estado de WhatsApp incompleto. wamid={}, recipient_id={}, status={}", messageId, waId, state);
                continue;
            }

            if ("failed".equals(state)) {
                logFailedStatusError(s);
            }

            Optional<UserChatEntity> userOptional = userChatRepository.findByWhatsappPhone(waId);

            if (userOptional.isEmpty()) {
                logger.warn("Usuario no encontrado para estado de WhatsApp. waId={}, wamid={}", waId, messageId);
                continue;
            }

            UserChatEntity user = userOptional.get();

            if (user.isBlock()) {
                return;
            }

            Optional<MessageEntity> op = messageRepository.findByWamid(messageId);

            MessageEntity msg;

            if (op.isEmpty()) {
                logger.warn(
                        "⚠️ Mensaje con ID {} no encontrado en BD. Creando registro...",
                        messageId);

                msg = new MessageEntity();

                msg.setConversationUserPhone(waId);
                msg.setFromPhone(businessPhoneNumber);
                msg.setToPhone(waId);
                msg.setWamid(messageId);
                if ("failed".equals(state)) {
                    msg.setFailedAt(parseWhatsappTimestamp(timestamp));
                }
                msg.setTimestamp(Instant.now());
                msg.setDirection(MessageDirectionEnum.OUTBOUND);
                msg.setSource(MessageSourceEnum.UNKNOWN);

                attachFirstStatusError(msg, s);

                msg = messageRepository.save(msg);
            } else {
                msg = op.get();
            }

            Instant ts = parseWhatsappTimestamp(timestamp);
            boolean shouldNotifyRead = false;

            switch (state) {
                case "sent" -> {
                    if (msg.getSentAt() == null)
                        msg.setSentAt(ts);
                }
                case "delivered" -> {
                    if (msg.getDeliveredAt() == null)
                        msg.setDeliveredAt(ts);
                }
                case "read" -> {
                    if (msg.getReadAt() == null) {
                        msg.setReadAt(ts);
                        shouldNotifyRead = true;
                    }
                }
                case "failed" -> {
                    msg.setFailedAt(ts);
                    attachFirstStatusError(msg, s);

                }
                default -> logger.debug("Estado no manejado: {}", state);
            }

            Optional<WhatsAppDataDto.Pricing> pricingOptional = Optional.ofNullable(s.pricing()).orElse(Optional.empty());
            if (pricingOptional.isPresent()) {
                var p = pricingOptional.get();
                MessageEntity messageForPricing = msg;

                MessagePricingEntity pricing = messagePricingRepository.findByMessageId(msg.getId())
                        .orElseGet(() -> {
                            MessagePricingEntity x = new MessagePricingEntity();
                            x.setMessage(messageForPricing);
                            return x;
                        });

                pricing.setPricingBillable(p.billable());
                pricing.setPricingModel(p.pricing_model());
                pricing.setPricingCategory(p.category());
                pricing.setPricingType(p.type());

                messagePricingRepository.save(pricing);
            }

            try {
                msg = messageRepository.save(msg);
                if (shouldNotifyRead) {
                    notifyOutboundMessageRead(msg);
                }
            } catch (Exception e) {
                logger.error("Error al guardar estado {} para mensaje {}: {}", state, messageId, e.getMessage());
            }
        }
    }

    // ============== Eliminar archvio multi-media por ID ===================
    @Override
    public Boolean deleteMediaById(String mediaId) {
        try {
            // Construir la URL manualmente sin el identifier
            String url = "https://graph.facebook.com/v23.0/" + mediaId;

            String response = restClient.delete()
                .uri(url)
                .retrieve()
                .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            return root.path("success").asBoolean();
        } catch (Exception e) {
            logger.error("Error al eliminar el archivo multimedia " + mediaId + ": ", e);
            return false;
        }
    }

    // ============== Obtener Media ============
    @Override
    public ResponseMediaMetadata getMediaMetadata(String mediaId) {
        try {
            String body = restMediaClient.get()
                .uri("/{mediaId}", mediaId)
                .retrieve()
                .body(String.class);
            return objectMapper.readValue(body, ResponseMediaMetadata.class);

        } catch (RestClientResponseException e) {
             if (e.getStatusCode().value() == 400) {
                    throw new MediaNotFoundException(
                    "media_id no encontrado o expirado: " + mediaId,
                    e
                    );
                }
                throw new ServerClientException(
                "Error al consultar metadata de media_id: " + e.getResponseBodyAsString(),
                e
                );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("No se pudo parsear JSON de metadata de media", e);
        }
    }

    // ============== Enviar plantilla de feedback ==================
    @Override
    public ResponseWhatsapp sendTemplatefeedback(String toPhoneNumber) {
        final String TEMPLATE_NAME = "feedback_de_catia";
        RequestMessages tpl = RequestMessagesFactory.buildTemplateMessage(
            toPhoneNumber,
            TEMPLATE_NAME,
            "es",
            "https://ia-sp-backoffice.ucatolica.cue.ec/uploads/catia_feedback.png",
            "CatIA",
            "Universidad Católica de Cuenca",
            "TAKE_SURVEY"
        );
        ResponseWhatsapp resp = NewResponseBuilder(tpl, "/messages");

        if (resp == null || resp.messages() == null || resp.messages().isEmpty()) {
            logger.warn("⚠️ API WhatsApp devolvió respuesta sin messages[] al enviar template {}", TEMPLATE_NAME);
            return resp;
        }

        ResponseWhatsappMessage msgResp = resp.messages().get(0);
        String messageStatus = Optional.ofNullable(msgResp.messageStatus()).orElse("UNKNOWN");

        MessageBody body = new MessageBody(
                toPhoneNumber,
                null,
                "System",
                MessageSourceEnum.BACK_END,
                businessPhoneNumber,
                MessageTypeEnum.TEMPLATE,
                null);

        MessageEntity messageEntity = MessageMapperHelper.createSentMessageEntity(body, resp);
        messageEntity = messageRepository.save(messageEntity);

        // Guardar registro de plantilla vinculado al mensaje
        MessageTemplateEntity templateMessage = new MessageTemplateEntity();
        templateMessage.setTemplateName(TEMPLATE_NAME);
        templateMessage.setMessageStatus(messageStatus);
        templateMessage.setAnsweredAt(null);
        templateMessage.setAnswer(null);
        templateMessage.setMessage(messageEntity);
        templateMsgRepo.save(templateMessage);
        notifyMessageUpdate(messageEntity);

        return resp;
    }

    // ============== Convertir entidad plantilla a DTO ==================
    private ResponseMessageTemplate templateMessageEntitytoDto(MessageTemplateEntity template) {
        MessageEntity msg = template.getMessage();

        String toPhone = null;
        String wamid = null;
        LocalDateTime sentAt = null;

        if (msg != null) {
            toPhone = msg.getToPhone();
            wamid = msg.getWamid();

            if (msg.getSentAt() != null) {
                sentAt = msg.getSentAt().atZone(ZoneOffset.UTC).toLocalDateTime();
            } else if (msg.getTimestamp() != null) {
                sentAt = msg.getTimestamp().atZone(ZoneOffset.UTC).toLocalDateTime();
            }
        }

        return new ResponseMessageTemplate(
                template.getId(),
                toPhone,
                template.getTemplateName(),
                sentAt,
                template.getAnsweredAt(),
                wamid,
                template.getAnswer(),
                template.getMessageStatus());
    }

    // ============== Obtener plantillas ==================
    @Override
    @Transactional
    public Page<ResponseMessageTemplate> getResponsesTemplate(Pageable pageable, Boolean onlyAnswered) {
        Page<MessageTemplateEntity> pageResult;
        if (onlyAnswered == null || !onlyAnswered) {
            pageResult = templateMsgRepo.findAllWithMessages(pageable);
        } else {
            pageResult = templateMsgRepo.findAnsweredWithMessages(pageable);
        }
        return pageResult.map(this::templateMessageEntitytoDto);
    }

    // ============== Obtener plantilla por fecha de envío ==================
    @Override
    @Transactional
    public List<ResponseMessageTemplate> listResponseTemplateByDate(LocalDateTime inicio, LocalDateTime fin) {
        LocalDateTime startOfDay = inicio.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = fin.toLocalDate().atTime(LocalTime.MAX);

        Instant start = startOfDay.atZone(ZoneOffset.UTC).toInstant();
        Instant end = endOfDay.atZone(ZoneOffset.UTC).toInstant();

        return templateMsgRepo.findByMessageTimestampBetween(start, end).stream()
                .map(this::templateMessageEntitytoDto)
                .collect(Collectors.toList());
    }

    // ============== Obtener plantilla por nombre ==================
    @Override
    @Transactional
    public List<ResponseMessageTemplate> listResponseTemplateByName(String templateName) {
        return templateMsgRepo.findByTemplateName(templateName).stream()
            .map(this::templateMessageEntitytoDto)
            .collect(Collectors.toList());
    }

    // ============== Obtener plantilla por usuario ==================
    @Override
    @Transactional
    public List<ResponseMessageTemplate> listResponseTemplateByPhone(String whatsAppPhone) {
        return templateMsgRepo.findByMessageToPhone(whatsAppPhone).stream()
                .map(this::templateMessageEntitytoDto)
                .collect(Collectors.toList());
    }

    // ============== Enviar una imagen por ID ==================
    @Override
    public ResponseWhatsapp sendImageMessageById(MessageBody payload, String mediaId) {
        try {
            RequestMessages msj = RequestMessagesFactory.buildImageById(payload.number(), mediaId, payload.message(), payload.contextId());
            ResponseWhatsapp res = NewResponseBuilder(msj, "/messages");

            try { Thread.sleep(150); } catch (InterruptedException ignored) {}

            if (res != null && !res.messages().isEmpty()) {
                MessageEntity entity = MessageMapperHelper.createSentMessageEntity(payload, res);
                entity.setMediaId(mediaId);
                entity = messageRepository.save(entity);
                notifyMessageUpdate(entity);
            }

            return res;

        } catch (Exception e) {
            logger.error("Error al enviar la imagen por ID: ", e);
            return null;
        }
    }
    
    // ============= Enviar una imagen por URL ================
    @Override
    public ResponseWhatsapp sendImageMessageByUrl(MessageBody payload, String imageUrl) {
        try {
            RequestMessages msj = RequestMessagesFactory.buildImageByUrl(payload.number(), imageUrl, payload.message(), payload.contextId());
            ResponseWhatsapp res = NewResponseBuilder(msj, "/messages");

            try { Thread.sleep(150); } catch (InterruptedException ignored) {}

            if (res != null && !res.messages().isEmpty()) {
                MessageEntity entity = MessageMapperHelper.createSentMessageEntity(payload, res);
                entity.setMediaUrl(imageUrl);
                entity = messageRepository.save(entity);
                notifyMessageUpdate(entity);
            }

            return res;

        } catch (Exception e) {
            logger.error("Error al enviar la imagen por URL: ", e);
            return null;
        }
    }

    // ============== Enviar un docuemnto por ID ==================
    @Override
    public ResponseWhatsapp sendDocumentMessageById(MessageBody payload, String documentId, String filename) {
        try {
            RequestMessages msj = RequestMessagesFactory.buildDocumentById(payload.number(), documentId, payload.message(), filename, payload.contextId());
            ResponseWhatsapp res = NewResponseBuilder(msj, "/messages");

            if (res != null && !res.messages().isEmpty()) {
                MessageEntity entity = MessageMapperHelper.createSentMessageEntity(payload, res);
                entity.setMediaId(documentId);
                entity.setMediaFilename(filename);
                entity = messageRepository.save(entity);
                notifyMessageUpdate(entity);
            }

            return res;

        } catch (Exception e) {
            logger.error("Error al enviar documento por ID: ", e);
            return null;
        }
    }

    // ============== Enviar un docuemnto por URL ==================
    @Override
    public ResponseWhatsapp sendDocumentMessageByUrl(MessageBody payload, String documentUrl, String filename){
        try {
            RequestMessages msj = RequestMessagesFactory.buildDocumentByUrl(payload.number(), 
                    documentUrl,
                    payload.message(), filename, payload.contextId());
            ResponseWhatsapp res = NewResponseBuilder(msj, "/messages");

            try { Thread.sleep(150); } catch (InterruptedException ignored) {}

            if (res != null && !res.messages().isEmpty()) {
                MessageEntity entity = MessageMapperHelper.createSentMessageEntity(payload, res);
                entity.setMediaUrl(documentUrl);
                entity.setMediaFilename(filename);
                entity = messageRepository.save(entity);
                notifyMessageUpdate(entity);
            }

            return res;

        } catch (Exception e) {
            logger.error("Error al enviar documento por URL: ", e);
            return null;
        }
    }

    // ============== Enviar una video por URL ============
    @Override
    public ResponseWhatsapp sendVideoMessageByUrl(MessageBody payload, String videoUrl) {
        try {
            RequestMessages msj = RequestMessagesFactory.buildVideoByUrl(payload.number(), videoUrl, payload.message(), payload.contextId());
            ResponseWhatsapp res = NewResponseBuilder(msj, "/messages");

            try { Thread.sleep(150); } catch (InterruptedException ignored) {}

            if (res != null && !res.messages().isEmpty()) {
                MessageEntity entity = MessageMapperHelper.createSentMessageEntity(payload, res);
                entity.setMediaUrl(videoUrl);
                entity = messageRepository.save(entity);
                notifyMessageUpdate(entity);
            }

            return res;

        } catch (Exception e) {
            logger.error("Error al enviar el video por URL: ", e);
            return null;
        }
    }

    // ================= Enviar un video por ID ==============
    @Override
    public ResponseWhatsapp sendVideoMessageById(MessageBody payload, String videoId) {
        try {
            RequestMessages msj = RequestMessagesFactory.buildVideoById(payload.number(), videoId, payload.message(), payload.contextId());
            ResponseWhatsapp res = NewResponseBuilder(msj, "/messages");

            try { Thread.sleep(150); } catch (InterruptedException ignored) {}

            if (res != null && !res.messages().isEmpty()) {
                MessageEntity entity = MessageMapperHelper.createSentMessageEntity(payload, res);
                entity.setMediaId(videoId);
                entity = messageRepository.save(entity);
                notifyMessageUpdate(entity);
            }

            return res;

        } catch (Exception e) {
            logger.error("Error al enviar el video por ID: ", e);
            return null;
        }
    }

    // ============== Enviar una Sticker statico/animado por URL ==============
    public ResponseWhatsapp sendStickerMessageByUrl(MessageBody payload, String stickerUrl) {
        try {
            RequestMessages msj = RequestMessagesFactory.buildStickerByUrl(payload.number(), stickerUrl, payload.contextId());
            ResponseWhatsapp res = NewResponseBuilder(msj, "/messages");

            try { Thread.sleep(150); } catch (InterruptedException ignored) {}

            if (res != null && !res.messages().isEmpty()) {
                MessageEntity entity = MessageMapperHelper.createSentMessageEntity(payload, res);
                entity.setMediaUrl(stickerUrl);
                entity = messageRepository.save(entity);
                notifyMessageUpdate(entity);
            }

            return res;

        } catch (Exception e) {
            logger.error("Error al enviar el sticker: ", e);
            return null;
        }
    }

}
