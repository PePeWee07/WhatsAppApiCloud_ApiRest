package com.BackEnd.WhatsappApiCloud.controller;

import java.io.File;
import java.io.IOException;

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

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.media.ResponseMediaMetadata;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.webhookEvents.WhatsAppDataDto;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.ApiWhatsappService;
import com.fasterxml.jackson.core.JsonProcessingException;


@RestController
@RequestMapping("/api/v1/whatsapp")
public class WhatsappController {

    private final ApiWhatsappService apiWhatsappService;

    public WhatsappController(ApiWhatsappService apiWhatsappService) {
        this.apiWhatsappService = apiWhatsappService;
    }

    
    // ======================================================
    //   Enviar mensaje a un usuario de WhatsApp especifico
    // ======================================================
    @PostMapping("/send")
    public ResponseEntity<ResponseWhatsapp> enviar(@RequestBody MessageBody payload) {
        try {
            ResponseWhatsapp response = apiWhatsappService.sendMessage(payload);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    // ======================================================
    //   Recibir mensaje de un usuario de WhatsApp
    // ======================================================
    @PostMapping("/receive")
    public ResponseWhatsapp receiveMessage(@RequestBody WhatsAppDataDto.WhatsAppMessage message) throws JsonProcessingException {
        if(message.entry().get(0).changes().get(0).value().messages() != null){
            System.out.println("Mensaje recibido: " + message.entry().get(0).changes().get(0).value().messages().get(0).text()); //! Debug
            return apiWhatsappService.handleUserMessage(message);
        }
        return null;
    }


    // ======================================================
    //   Cargar archivo multimedia a server de WhatsApp
    // ======================================================
    @PostMapping("/upload-media-file")
    public ResponseEntity<?> uploadMedia(@RequestParam("file") MultipartFile file) {
        try {
            // Guardar temporalmente el archivo en el sistema
            File tempFile = File.createTempFile("upload_", file.getOriginalFilename());
            file.transferTo(tempFile);
            String mediaId = apiWhatsappService.uploadMedia(tempFile);
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

    // ======================================================
    //   Eliminar un archvio multimedia del server de WhatsApp
    // ======================================================
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

    
    // ======================================================
    //   Enviar imagen por ID con texto a un usuario de WhatsApp
    // ======================================================
    @PostMapping("/send-image-by-id")
    public ResponseEntity<ResponseWhatsapp> sendImageByIdWithText(
            @RequestParam("toPhoneNumber") String toPhoneNumber,
            @RequestParam("mediaId") String mediaId,
            @RequestParam("caption") String caption) {
        try {
            ResponseWhatsapp response = apiWhatsappService.sendImageMessageById(toPhoneNumber, mediaId, caption);
            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    // ======================================================
    //   Enviar docuemnto por ID con texto a un usuario de WhatsApp
    // ======================================================
    @PostMapping("/send-document-by-id")
    public ResponseEntity<ResponseWhatsapp> sendDocumentByIdWithText(
            @RequestParam("toPhoneNumber") String toPhoneNumber,
            @RequestParam("mediaId") String mediaId,
            @RequestParam("caption") String caption,
            @RequestParam("filename") String filename) {
        try {
            ResponseWhatsapp response = apiWhatsappService.sendDocumentMessageById(toPhoneNumber, mediaId, caption, filename);
            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/media/{mediaId}")
    public ResponseEntity<ResponseMediaMetadata> getMediaMetadata(@PathVariable String mediaId) {
        ResponseMediaMetadata meta = apiWhatsappService.getMediaMetadata(mediaId);
        return ResponseEntity.ok(meta);
    }

    @PostMapping("/template-feedback")
    public ResponseEntity<ResponseWhatsapp> sendFeedbackTemplate(
            @RequestParam("to") String toPhoneNumber) {

        ResponseWhatsapp response = apiWhatsappService.sendTemplatefeedback(toPhoneNumber);
        if (response == null) {
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.ok(response);
    }

}
