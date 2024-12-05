package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ApiWhatsappServiceImpl implements ApiWhatsappService {

    private static final Logger logger = LoggerFactory.getLogger(ApiWhatsappServiceImpl.class);

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
            logger.error("Error al enviar mensaje: " + e);
            return null;
        }
    }

    // Metodo Recibir y enviar respuesta automatica
    @Override
    public ResponseWhatsapp handleUserMessage(WhatsAppData.WhatsAppMessage message) {
        LocalDateTime timeNow = LocalDateTime.now();
        String messageType = message.entry().get(0).changes().get(0).value().messages().get(0).type();
        String waId = message.entry().get(0).changes().get(0).value().contacts().get(0).wa_id();
        String messageText = message.entry().get(0).changes().get(0).value().messages().get(0).text().get().body();

        if (!messageType.equals("text") || messageText == null || messageText.isEmpty()) {
            return sendSimpleResponse(waId, "Lo sentimos, no es posible procesar este tipo de mensaje. Por favor, verifica el formato o el contenido e inténtalo nuevamente.");
        }

        try {
            UserChatEntity user = userChatRepository.findByPhone(waId)
                .orElseGet(() -> {
                    //! Si no existe el usuario en mi BD, lo creo
                    UserChatEntity newUser = new UserChatEntity();
                    newUser.setPhone(waId);
                    newUser.setNombres("Anonymus");
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
                            user.setNombres("Usuario");
                            user.setConversationState("READY");
                            user.setRol("Invitado");
                            userChatRepository.save(user);
                            return sendSimpleResponse(waId, "Actualmente estás en modo invitado y no perteneces a la universidad. ¿En qué puedo ayudarte?");
                        } 
                        //! Si encuentro la cédula dentro de ERP
                        else {
                            user.setLastInteraction(timeNow);
                            user.setNombres(userFromJsonServer.getNombres());
                            user.setCarrera(userFromJsonServer.getCarrera());
                            user.setCedula(userFromJsonServer.getCedula());
                            user.setConversationState("READY");
                            user.setRol(userFromJsonServer.getRol());
                            user.setSede(userFromJsonServer.getSede());
                            userChatRepository.save(user);
                            return sendSimpleResponse(waId, "Hola " + user.getNombres() + ", bienvenido al Asistente Tecnológico de TICs. ¿En qué puedo ayudarte hoy?");
                        }

                    } else {
                        return sendSimpleResponse(waId, "Por favor, introduce tu número de cédula valida para continuar.");
                    }
                case "READY":

                    //! 1. Verifica si ya pasaron 24 horas para incremetar el límite y restablecer el contador
                    if (!user.getLastInteraction().toLocalDate().isEqual(LocalDate.now())) {
                        user.setLimite(10);
                        user.setNextResetDate(null);
                        userChatRepository.save(user);
                    }

                    //! 2. Si ya pasó la fecha de reseteo, restablece el límite
                    if (user.getNextResetDate() != null && timeNow.isAfter(user.getNextResetDate())) {
                        user.setLimite(10);
                        user.setNextResetDate(null);
                        userChatRepository.save(user);
                    }

                    //! 3. Si hay una fecha de reseteo definida y aún no ha pasado, no puede realizar la acción
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

                    //! 4. Si el límite de interacciones es 0, bloquear por 24 horas
                    if (user.getLimite() <= 0) {
                        user.setNextResetDate(timeNow.plusHours(24));
                        userChatRepository.save(user);
                        return sendSimpleResponse(waId, "Tu límite de interacciones ha sido alcanzado, vuelve mañana.");
                    }

                    //! 5. Actualizar datos del usuario y obtener respuesta de IA
                    UserChatEntity userFromJsonServer = fetchUserFromJsonServer(user.getCedula());
                    user.setNombres(userFromJsonServer.getNombres());
                    user.setCarrera(userFromJsonServer.getCarrera());
                    user.setRol(userFromJsonServer.getRol());
                    user.setSede(userFromJsonServer.getSede());

                    AnswersOpenIa data = getAnswerIA(messageText, user.getNombres(), user.getThread_id());

                    user.setLastInteraction(timeNow);
                    user.setThread_id(data.thread_id());
                    user.setLimite(user.getLimite() - 1);
                    userChatRepository.save(user);

                    return sendSimpleResponse(waId, data.respuesta());
                default:
                    user.setConversationState("WAITING_FOR_CEDULA");
                    userChatRepository.save(user);
                    return sendSimpleResponse(waId, "No hemos podido procesar tu solicitud. Por favor, introduce tu número de cédula nuevamente para continuar.");
            }

        } catch (Exception e) {
            logger.error("Error al procesar mensaje de usuario: " + e);
            return sendSimpleResponse(waId, "Ha ocurrido un error inesperado. Por favor, inténtalo nuevamente más tarde.");
        }
    }

    private static boolean isValidCedula(String cedula) {
        // Validar que tenga 10 dígitos
        if (cedula == null || cedula.length() != 10) {
            return false;
        }
    
        try {
            // Obtener los dos primeros dígitos (región)
            int digitoRegion = Integer.parseInt(cedula.substring(0, 2));
    
            // Validar región (1 a 24)
            if (digitoRegion < 1 || digitoRegion > 24) {
                return false;
            }
    
            // Extraer el último dígito
            int ultimoDigito = Integer.parseInt(cedula.substring(9, 10));
    
            // Sumar los pares
            int pares = Integer.parseInt(cedula.substring(1, 2)) +
                        Integer.parseInt(cedula.substring(3, 4)) +
                        Integer.parseInt(cedula.substring(5, 6)) +
                        Integer.parseInt(cedula.substring(7, 8));
    
            // Sumar los impares, multiplicando por 2 y ajustando si > 9
            int impares = sumarImpar(cedula.substring(0, 1)) +
                          sumarImpar(cedula.substring(2, 3)) +
                          sumarImpar(cedula.substring(4, 5)) +
                          sumarImpar(cedula.substring(6, 7)) +
                          sumarImpar(cedula.substring(8, 9));
    
            // Suma total
            int sumaTotal = pares + impares;
    
            // Obtener el primer dígito de la suma total
            int primerDigitoSuma = Integer.parseInt(String.valueOf(sumaTotal).substring(0, 1));
    
            // Obtener la decena inmediata
            int decena = (primerDigitoSuma + 1) * 10;
    
            // Calcular el dígito validador
            int digitoValidador = decena - sumaTotal;
    
            // Si el dígito validador es 10, se ajusta a 0
            if (digitoValidador == 10) {
                digitoValidador = 0;
            }
    
            // Validar el dígito validador contra el último dígito
            return digitoValidador == ultimoDigito;
    
        } catch (NumberFormatException e) {
            logger.info(cedula + " no es un número válido.");
            return false;
        }
    }
    private static int sumarImpar(String numero) {
        int valor = Integer.parseInt(numero) * 2;
        return (valor > 9) ? (valor - 9) : valor;
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

            System.out.println("Users: " + localRestClient.get().uri(url).retrieve().body(String.class));
            
            // Buscar el rol que no sea "Estudiante"
            UserChatEntity nonStudentRole = users.stream()
                .filter(user -> !user.getRol().equalsIgnoreCase("Estudiante"))
                .findFirst().orElse(users.get(0));

            return nonStudentRole;
        } catch (Exception apiException) {
            logger.error("Error al obtener datos del usuario desde ERP: " + apiException.getMessage());
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
            logger.error("Error al obtener respuesta de IA: " + e.getMessage());
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
            logger.error("Error al procesar JSON: " + e);
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
            logger.error("Error al construir mensaje de petición: " + e);
            return null;
        }
    }
 
}
