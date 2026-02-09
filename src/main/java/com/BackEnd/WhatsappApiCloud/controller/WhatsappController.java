package com.BackEnd.WhatsappApiCloud.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageTemplateDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseMediaMetadata;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseMessageTemplate;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.webhookEvents.WhatsAppDataDto;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.ApiWhatsappService;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.MessageHistoryService;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.WhatsappMediaService;
import com.fasterxml.jackson.core.JsonProcessingException;


@RestController
@RequestMapping("/api/v1/whatsapp")
public class WhatsappController {

    private final ApiWhatsappService apiWhatsappService;
    private final WhatsappMediaService whatsappMediaService;
    private final MessageHistoryService messageHistoryService;
    private static final int MAX_PAGE_SIZE = 100;

    public WhatsappController(
        ApiWhatsappService apiWhatsappService,
        WhatsappMediaService whatsappMediaService, 
        MessageHistoryService messageHistoryService) {
        this.apiWhatsappService = apiWhatsappService;
        this.whatsappMediaService = whatsappMediaService;
        this.messageHistoryService = messageHistoryService;
    }

    
    // =================== Enviar mensaje a un usuario de WhatsApp especifico ===================
    @PostMapping("/send")
    public ResponseEntity<ResponseWhatsapp> sendMessageToWhatsApp(@RequestBody MessageBody payload) {
        try {
            ResponseWhatsapp response = apiWhatsappService.sendMessage(payload);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ================ Recibir mensaje de un usuario de WhatsApp ========================
    @PostMapping("/receive-message")
    public ResponseWhatsapp receiveMessage(@RequestBody WhatsAppDataDto.WhatsAppMessage message) throws JsonProcessingException {
        System.out.println("Mensaje recibido: " + message.entry().get(0).changes().get(0).value().messages().get(0).text()); //! Debug
        return apiWhatsappService.handleUserMessage(message);
    }

    @PostMapping("/receive-message-status")
    public ResponseEntity<String> receiveMessageStatus(@RequestBody WhatsAppDataDto.WhatsAppMessage message) throws JsonProcessingException {
        System.out.println("Estado de mensaje recibido: " + message.entry().get(0).changes().get(0).value().statuses().get(0).status()); //! Debug
        apiWhatsappService.handleMessageStatus(message);
        return ResponseEntity.ok("Status processed successfully");
    }

    // ================= Cargar archivo multimedia a server de WhatsApp =======================
    @PostMapping("/upload-media-file")
    public ResponseEntity<?> uploadMedia(@RequestParam("file") MultipartFile file) {
        try {
            // Guardar temporalmente el archivo en el sistema
            File tempFile = File.createTempFile("upload_", file.getOriginalFilename());
            file.transferTo(tempFile);
            String mediaId = whatsappMediaService.uploadMedia(tempFile);
            tempFile.delete();

            if (mediaId != null) {
                return ResponseEntity.ok(mediaId);
            } else {
                return ResponseEntity.status(500).body("Error al subir la imagen.");
            }

        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error al procesar el archivo: " + e.getMessage());
        }
    }

    // =============== Eliminar un archvio multimedia del server de WhatsApp ==================
    @DeleteMapping("/delete-media-file")
    public ResponseEntity<?> deleteMedia(@RequestParam("mediaId") String mediaId) {
        try {
            if (mediaId == null || mediaId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El mediaId no puede estar vacío.");
            }

            boolean deleted = apiWhatsappService.deleteMediaById(mediaId);
            if (deleted) {
                return ResponseEntity.ok("Archivo multimedia eliminado correctamente.");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El archivo multimedia no se pudo encontrar o eliminar.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar la solicitud: " + e.getMessage());
        }
    }
    
    // ============ Enviar imagen por ID ==============
    @PostMapping("/send-image-by-id")
    public ResponseEntity<ResponseWhatsapp> sendImageByI(
            @RequestBody MessageBody payload,
            @RequestParam String imageId) {
        try {
            ResponseWhatsapp response = apiWhatsappService.sendImageMessageById(payload, imageId);
            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ============ Enviar imagen por URL ==============
    @PostMapping("/send-image-by-url")
    public ResponseEntity<ResponseWhatsapp> sendImageByUrl(
            @RequestBody MessageBody payload,
            @RequestParam String imageUrl) {
        try {
            ResponseWhatsapp response = apiWhatsappService.sendImageMessageByUrl(payload, imageUrl);
            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ============ Enviar video por ID ================
    @PostMapping("/send-video-by-id")
    public ResponseEntity<ResponseWhatsapp> sendVideoById(
            @RequestBody MessageBody payload,
            @RequestParam String videoId) {
        try {
            ResponseWhatsapp response = apiWhatsappService.sendVideoMessageById(payload, videoId);
            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ============ Enviar video por URL ================
    @PostMapping("/send-video-by-url")
    public ResponseEntity<ResponseWhatsapp> sendVideoByUrl(
        @RequestBody MessageBody payload,
        @RequestParam String videoUrl) {
        try {
            ResponseWhatsapp response = apiWhatsappService.sendVideoMessageByUrl(payload, videoUrl);
            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // =============== Enviar documento por ID ==================
    @PostMapping("/send-document-by-id")
    public ResponseEntity<ResponseWhatsapp> sendDocumentById(
            @RequestBody MessageBody payload,
            @RequestParam String documentId,
            @RequestParam String filename) {
        try {
            ResponseWhatsapp response = apiWhatsappService.sendDocumentMessageById(payload, documentId, filename);
            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // =============== Enviar documento por URL ==================
    @PostMapping("/send-document-by-url")
    public ResponseEntity<ResponseWhatsapp> sendDocumentByurl(
            @RequestBody MessageBody payload,
            @RequestParam String documentUrl,
            @RequestParam String filename) {
        try {
            ResponseWhatsapp response = apiWhatsappService.sendDocumentMessageByUrl(payload, documentUrl, filename);
            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ================ Obtener datos del archivo multimedia por ID =======================
    @GetMapping("/media/{mediaId}")
    public ResponseEntity<ResponseMediaMetadata> getMediaMetadata(@PathVariable String mediaId) {
        ResponseMediaMetadata meta = apiWhatsappService.getMediaMetadata(mediaId);
        return ResponseEntity.ok(meta);
    }

    // ================ Enviar feedback =======================
    @PostMapping("/template-feedback")
    public ResponseEntity<ResponseWhatsapp> sendFeedbackTemplate(
            @RequestParam("to") String toPhoneNumber) {

        ResponseWhatsapp response = apiWhatsappService.sendTemplatefeedback(toPhoneNumber);
        if (response == null) {
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.ok(response);
    }

    // ================ Obtener respuestas de un template por numero =======================
    @GetMapping("/template/{toPhone}")
    public ResponseEntity<List<ResponseMessageTemplate>> getTemplateByPhone(@PathVariable String toPhone) {
        List<ResponseMessageTemplate> list = apiWhatsappService.listResponseTemplateByPhone(toPhone);
        if (list.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(list);
    }

    // ================ Obtner todas las respuestas de los templates =======================
    @GetMapping("/template/all")
    public ResponseEntity<Page<ResponseMessageTemplate>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "message.sentAt") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            @RequestParam(defaultValue = "false") Boolean onlyAnswered
    ) {
        int size = Math.min(pageSize, MAX_PAGE_SIZE);
        Sort.Direction d = Sort.Direction.fromString(dir);
        Pageable pg = PageRequest.of(page, size, Sort.by(d, sort));
        return ResponseEntity.ok(apiWhatsappService.getResponsesTemplate(pg, onlyAnswered));
    }

    // ============== Obtener resultado de plantilla por fecha de envío ==================
    @GetMapping("/template/date-range")
    public ResponseEntity<List<ResponseMessageTemplate>> getTemplateByDateRange(
            @RequestParam("start") String start,
            @RequestParam("end") String end) {
        try {
            List<ResponseMessageTemplate> list = apiWhatsappService.listResponseTemplateByDate(
                    LocalDateTime.parse(start),
                    LocalDateTime.parse(end)
            );
            if (list.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // ============== Obtener resultado de plantilla por nombre ==================
    @GetMapping("/template/name/{templateName}")
    public ResponseEntity<List<ResponseMessageTemplate>> getTemplateByName(@PathVariable String templateName) {
        List<ResponseMessageTemplate> list = apiWhatsappService.listResponseTemplateByName(templateName);
        if (list.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/messages/{id}/template")
    public MessageTemplateDto getTemplateByMessageId(@PathVariable("id") Long messageId) {
        if (messageId == null || messageId <= 0) {
            throw new IllegalArgumentException("Invalid message ID");
        }
        return messageHistoryService.getMessageTemplateByMessageId(messageId);
    }

}
