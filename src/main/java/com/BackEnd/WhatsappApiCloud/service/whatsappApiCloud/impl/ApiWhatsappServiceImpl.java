package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.impl;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tika.Tika;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.BackEnd.WhatsappApiCloud.exception.ApiInfoException;
import com.BackEnd.WhatsappApiCloud.exception.ErpNotFoundException;
import com.BackEnd.WhatsappApiCloud.exception.MediaNotFoundException;
import com.BackEnd.WhatsappApiCloud.exception.ServerClientException;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.TemplateMessageDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestMessages;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestMessagesFactory;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.RequestWhatsappAsRead;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.media.ResponseMediaMetadata;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsappMessage;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.webhookEvents.WhatsAppDataDto;
import com.BackEnd.WhatsappApiCloud.model.dto.erp.ErpUserDto;
import com.BackEnd.WhatsappApiCloud.model.dto.erp.ErpRolUserDto;
import com.BackEnd.WhatsappApiCloud.model.dto.openIA.AnswersOpenIADto;
import com.BackEnd.WhatsappApiCloud.model.dto.openIA.QuestionOpenIADto;
import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.TemplateMessageEntity;
import com.BackEnd.WhatsappApiCloud.repository.TemplateMessageRepository;
import com.BackEnd.WhatsappApiCloud.repository.UserChatRepository;
import com.BackEnd.WhatsappApiCloud.service.erp.ErpCacheService;
import com.BackEnd.WhatsappApiCloud.service.erp.ErpServerClient;
import com.BackEnd.WhatsappApiCloud.service.openAi.openAiServerClient;
import com.BackEnd.WhatsappApiCloud.service.userChat.ChatHistoryService;
import com.BackEnd.WhatsappApiCloud.service.userChat.UserChatSessionService;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.ApiWhatsappService;
import com.BackEnd.WhatsappApiCloud.util.ConversationState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ApiWhatsappServiceImpl implements ApiWhatsappService {

    private static final Logger logger = LoggerFactory.getLogger(ApiWhatsappServiceImpl.class);

    private final RestClient restClient;
    private final RestClient restMediaClient;
    private final ObjectMapper objectMapper;

    private static final String TEMPLATE_NAME               = "feedback_de_catia";
    private static final String TEMPLATE_IMAGE_CLASSPATH   = "templates/catia_feedback.png";

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
    ErpServerClient erpJsonServerClient;
    @Autowired
    openAiServerClient openAiServerClient;
    @Autowired
    UserChatSessionService chatSessionService;
    @Autowired
    ErpCacheService erpCacheService;
    @Autowired
    ChatHistoryService chatHistoryService;
    @Autowired
    private TemplateMessageRepository templateMsgRepo;

    // ================ Constructor para inicializar el cliente REST =====================
    public ApiWhatsappServiceImpl(
        @Value("${Phone-Number-ID}") String identifier,
        @Value("${whatsapp.token}") String token,
        @Value("${whatsapp.urlbase}") String urlBase,
        @Value("${whatsapp.version}") String version,
        ObjectMapper objectMapper) {

        restClient = RestClient.builder()
                    .baseUrl(urlBase + version + "/" + identifier)
                    .defaultHeader("Authorization", "Bearer " + token)
                    .build();

        restMediaClient = RestClient.builder()
                          .baseUrl(urlBase + version)
                          .defaultHeader("Authorization", "Bearer " + token)
                          .build();

        this.objectMapper = objectMapper;
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


    // =================== Envio de mensaje ==========================
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
        newUser.setConversationState(ConversationState.NEW);
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
            welcomeMessage = "Mensaje de bienvenidad no econtrado. Por favor, reporta a soportetic@ucaue.edu.ec.";
        }
        sendStickerMessageByUrl(waId, "https://almacenamiento.ucacue.edu.ec/videos/VA-with-logo-uc-Photoroom-ezgif.com-png-to-webp-converter.webp");
        sendMessage(new MessageBody(waId, welcomeMessage));
        user.setConversationState(ConversationState.ASKED_FOR_CEDULA);
        userChatRepository.save(user);
        return sendMessage(new MessageBody(waId, "Para comenzar, por favor, *ingresa tu número de cédula o identificación* 🔒."));
    }


    // ============== Estado "WAITING_FOR_CEDULA" ====================
    @Transactional
    private ResponseWhatsapp handleWaitingForCedula(UserChatEntity user, String messageText, String waId, LocalDateTime timeNow) {
        ErpUserDto dto;
        try {
            dto = erpJsonServerClient.getUser(messageText);
        } catch (ErpNotFoundException e) {
            user.setLastInteraction(timeNow);
            user.setLimitQuestions(user.getLimitQuestions() - 1);
            userChatRepository.save(user);
            return sendMessage(new MessageBody(
                waId,
                "No encontramos tu número de identificación en nuestro sistema. " +
                "Te quedan " + user.getLimitQuestions() + " intentos."
            ));
        } catch (ServerClientException e) {
            logger.warn("ERP no disponible al buscar identificación {}: {}", messageText, e.getMessage());
            return sendMessage(new MessageBody(
                waId,
                "Disculpa, nuestro servicio de verificación está temporalmente fuera de línea. " +
                "Por favor inténtalo de nuevo en unos minutos."
            ));
        }

        if (dto == null || dto.getIdentificacion() == null) {
            user.setLastInteraction(timeNow);
            user.setLimitQuestions(user.getLimitQuestions() - 1);
            userChatRepository.save(user);
            return sendMessage(new MessageBody(
                waId,
                "No encontramos tu número de identificación. " +
                "Te quedan " + user.getLimitQuestions() + " intentos."
            ));
        }

        // Encontró el usuario en el ERP
        user.setIdentificacion(dto.getIdentificacion());
        user.setLastInteraction(timeNow);
        user.setConversationState(ConversationState.READY);
        user.setLimitQuestions(limitQuestionsPerDay);
        user.setLimitStrike(strikeLimit);
        user.setNextResetDate(null);
        userChatRepository.save(user);

        return sendMessage(new MessageBody(
            waId,
            "¡Hola 🫡, " + dto.getNombres() + " " + dto.getApellidos() + "! ¿En qué puedo ayudarte hoy?"
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


    // ================ Estado "READY" =========================
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
        List<String> userRoles = userDto.getRolesUsuario().stream().map(ErpRolUserDto::getTipoRol).collect(Collectors.toList());

        QuestionOpenIADto question = new QuestionOpenIADto(
            messageText,
            userDto.getNombres() + " " + userDto.getApellidos(),
            waId,
            userRoles,
            user.getPreviousResponseId(),
            user.getIdentificacion(),
            userDto.getEmailInstitucional(),
            userDto.getEmailPersonal(),
            userDto.getSexo()
        );

        AnswersOpenIADto data = openAiServerClient.getOpenAiData(question);
        chatHistoryService.saveHistory(data, waId);

        user.setPreviousResponseId(data.previousResponseId());
        user.setLimitQuestions(user.getLimitQuestions() - 1);
        user.setLastInteraction(timeNow);
        user.setValidQuestionCount(user.getValidQuestionCount() + 1);
        userChatRepository.save(user);

        return sendMessage(new MessageBody(waId, data.answer()));
    }

    // ================ LLegada de Exepeciones Informativas o Moderación de IA ===================
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


    // =================== Recibir y enviar respuesta automática ======================
    @Override
    public ResponseWhatsapp handleUserMessage(WhatsAppDataDto.WhatsAppMessage message) {
        LocalDateTime timeNow = LocalDateTime.now();

        // Extraer datos básicos del mensaje
        var changeValue = message.entry().get(0).changes().get(0).value();
        String wamid            = changeValue.messages().get(0).id();
        String messageType      = changeValue.messages().get(0).type();
        String waId             = changeValue.contacts().get(0).wa_id();
        var messageOptionalText = changeValue.messages().get(0).text();

        // Marcar el mensaje como leído
        markAsRead(new RequestWhatsappAsRead("whatsapp", "read", wamid));

        // Para obtener respuesta de la plantilla de msg
        if (messageType.equals("interactive")) {
            var msg = changeValue.messages().get(0);
            var ctx = msg.context();
            Object rawInteractive = msg.interactive();
            String ts = msg.timestamp();

            long epochSec = Long.parseLong(ts);
            LocalDateTime answeredAt = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(epochSec),
                ZoneId.systemDefault()
            );

            if (ctx != null && rawInteractive instanceof Map<?,?> interactiveMap) {
                Object nfmObj = interactiveMap.get("nfm_reply");
                if (nfmObj instanceof Map<?,?> nfmMap) {
                    String parentWamid  = ctx.id();
                    String answerJson   = (String) nfmMap.get("response_json");
                    templateMsgRepo.findByWamid(parentWamid).ifPresent(log -> {
                        log.setAnswer(answerJson);
                        log.setAnsweredAt(answeredAt);
                        templateMsgRepo.save(log);
                    });
                }
            }
            return null;
        }

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
                        "¡Ups! 🫢 Algo inesperado ocurrió. Reiniciemos. \n"
                    + "Por favor, Escribe un mensaje para comenzar."));
                }
            }
        } catch (ApiInfoException e) {
            return handleApiInfoException(e, waId);
        } catch (Exception e) {
            logger.error("Error al procesar mensaje de usuario: ", e);
            return sendMessage(new MessageBody(
                waId,
                "Ha ocurrido un error inesperado 😟. Por favor, inténtalo nuevamente más tarde."
            ));
        }
    }


    // ============== Método para convertir CSV a Excel ==============
    private File convertCsvToExcel(File csvFile) throws IOException {
        File excelFile = new File(csvFile.getParent(), "converted_" + csvFile.getName() + ".xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            List<String> lines = Files.readAllLines(csvFile.toPath(), StandardCharsets.UTF_8);

            int rowIndex = 0;
            for (String line : lines) {
                Row row = sheet.createRow(rowIndex++);
                String[] cells = line.split(",");
                for (int cellIndex = 0; cellIndex < cells.length; cellIndex++) {
                    Cell cell = row.createCell(cellIndex);
                    cell.setCellValue(cells[cellIndex]);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }

        return excelFile;
    }


    // ============== Cargar archivo multimedia a servidores WhatsApp ===============
    @Override
    public String uploadMedia(File mediaFile) {
        try {
            if (mediaFile.length() == 0 || !mediaFile.exists()) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El archivo está vacío."
                );
            }

            Tika tika = new Tika();
            String contentType = tika.detect(mediaFile);

            if ("text/csv".equals(contentType)) {
                mediaFile = convertCsvToExcel(mediaFile);
                contentType = "application/vnd.ms-excel";
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(mediaFile));
            body.add("type", contentType);
            body.add("messaging_product", "whatsapp");

            String response = restClient.post()
                    .uri("/media")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            return root.path("id").asText();

        } catch (IOException e) {
            logger.error("Error al leer el archivo: ", e);
            throw new RuntimeException("Error al leer el archivo: ", e);
        } catch (Exception e) {
            logger.error("Error inesperado al subir el archivo: ", e);
            throw new RuntimeException("Error inesperado al subir el archivo al servidor de Whatsapp: ", e);
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


    // ============== Enviar una imagen por ID junto a un mensaje ==================
    @Override
    public ResponseWhatsapp sendImageMessageById(String toPhoneNumber, String mediaId, String caption) {
        try {
            RequestMessages msj = RequestMessagesFactory.buildImageByIdWithText(toPhoneNumber, mediaId, caption);

            ResponseWhatsapp res = NewResponseBuilder(msj, "/messages");
            return res;

        } catch (Exception e) {
            logger.error("Error al enviar la imagen con texto: ", e);
            return null;
        }
    }


    // ============== Enviar un docuemnto por ID junto a un mensaje ==================
    @Override
    public ResponseWhatsapp sendDocumentMessageById(String toPhoneNumber, String documentId, String caption, String filename) {
        try {
            RequestMessages msj = RequestMessagesFactory.buildDocumentByIdWithText(toPhoneNumber, documentId, caption, filename);

            ResponseWhatsapp res = NewResponseBuilder(msj, "/messages");
            return res;

        } catch (Exception e) {
            logger.error("Error al enviar documento con texto: ", e);
            return null;
        }
    }


    // ============== Enviar plantilla de mensaje ==================
    private File getTemplateImageFile() {
        // Extrae el recurso embebido y lo vuelca a un File temporal
        try {
            Resource res = new ClassPathResource(TEMPLATE_IMAGE_CLASSPATH);
            InputStream is = res.getInputStream();
            String ext = ".png";
            File tmp = File.createTempFile("tpl_", ext);
            Files.copy(is, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tmp;
        } catch (IOException e) {
            throw new IllegalStateException(
                "No se pudo cargar la plantilla de feedback desde classpath: " + TEMPLATE_IMAGE_CLASSPATH, e);
        }
    }

    @Cacheable(cacheNames="mediaIdCache", key="'" + TEMPLATE_NAME + "'")
    public String loadTemplateMediaId() {
        return uploadMedia(getTemplateImageFile());
    }

    @CacheEvict(cacheNames="mediaIdCache", key="'" + TEMPLATE_NAME + "'")
    public String evictAndReloadTemplateMediaId() {
        return loadTemplateMediaId();
    }

    @Override
    public ResponseWhatsapp sendTemplatefeedback(String toPhoneNumber) {
        String mediaId = loadTemplateMediaId();

        try {
            getMediaMetadata(mediaId);
        } catch (MediaNotFoundException ex) {
            mediaId = evictAndReloadTemplateMediaId();
        }

        RequestMessages tpl = RequestMessagesFactory.buildTemplateMessage(
            toPhoneNumber,
            TEMPLATE_NAME,
            "es",
            mediaId,
            "Catia",
            "Encuesta enviada por la Universidad Católica de Cuenca.",
            "TAKE_SURVEY"
        );
        ResponseWhatsapp resp = NewResponseBuilder(tpl, "/messages");

        // 3) extraemos el wamid de la respuesta
        String wamid = null;
        String messageStatus = "";
        if (resp != null && resp.messages() != null && !resp.messages().isEmpty()) {
            ResponseWhatsappMessage msg = resp.messages().get(0);
            wamid         = msg.id();
            messageStatus = Optional.ofNullable(msg.messageStatus()).orElse("UNKNOWN");

        }

        // 4) guardamos el log
        TemplateMessageEntity templateMessage = new TemplateMessageEntity();
        templateMessage.setToPhone(toPhoneNumber);
        templateMessage.setTemplateName(TEMPLATE_NAME);
        templateMessage.setSentAt(LocalDateTime.now());
        templateMessage.setWamid(wamid != null ? wamid : "UNKNOWN");
        templateMessage.setMessageStatus(messageStatus);
        templateMsgRepo.save(templateMessage);

        return resp;
    }


    // ============== Obtener resultados de plantillas ==================
    @Override
    public Page<TemplateMessageDto> getResponsesTemplate(Pageable pageable) {
        return templateMsgRepo.findAll(pageable).map(this::templateMessageEntitytoDto);
    }


    // ============== Obtener resultado de plantilla por WhatsAppPhone ==================
    @Override
    public List<TemplateMessageDto> listResponseTemplateByPhone(String WhatsAppPhone) {
        return templateMsgRepo.findByToPhone(WhatsAppPhone).stream()
            .map(this::templateMessageEntitytoDto)
            .collect(Collectors.toList());
    }

    private TemplateMessageDto templateMessageEntitytoDto(TemplateMessageEntity log) {
        return new TemplateMessageDto(
            log.getId(),
            log.getToPhone(),
            log.getTemplateName(),
            log.getSentAt(),
            log.getAnsweredAt(),
            log.getWamid(),
            log.getAnswer(),
            log.getMessageStatus()
        );
    }
    

    // ============= Enviar una imagen por URL como mensaje ================
    public ResponseWhatsapp sendImageMessageByUrl(String toPhoneNumber, String imageUrl) {
        try {
            RequestMessages msj = RequestMessagesFactory.buildImageByUrl(toPhoneNumber, imageUrl);

            ResponseWhatsapp res = NewResponseBuilder(msj, "/messages");
            return res;

        } catch (Exception e) {
            logger.error("Error al enviar la imagen: ", e);
            return null;
        }
    }


    // ============== Enviar una video por URL como mensaje ============
    public ResponseWhatsapp sendVideoMessageByUrl(String toPhoneNumber, String videoUrl, String caption) {
        try {
            RequestMessages msj = RequestMessagesFactory.buildVideoByUrl(toPhoneNumber, videoUrl, caption);

            ResponseWhatsapp res = NewResponseBuilder(msj, "/messages");
            return res;

        } catch (Exception e) {
            logger.error("Error al enviar el video: ", e);
            return null;
        }
    }


    // ============== Enviar una Sticker statico/animado por URL como mensaje ==============
    public ResponseWhatsapp sendStickerMessageByUrl(String toPhoneNumber, String stickerUrl) {
        try {
            RequestMessages msj = RequestMessagesFactory.buildStickerByUrl(toPhoneNumber, stickerUrl);

            ResponseWhatsapp res = NewResponseBuilder(msj, "/messages");
            return res;

        } catch (Exception e) {
            logger.error("Error al enviar el sticker: ", e);
            return null;
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
}
