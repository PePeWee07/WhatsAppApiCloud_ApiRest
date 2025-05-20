package com.BackEnd.WhatsappApiCloud.controller;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.BackEnd.WhatsappApiCloud.config.ApiKeyFilter;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.ResponseWhatsapp;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.webhookEvents.WhatsAppDataDto;
import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.BackEnd.WhatsappApiCloud.service.userChat.UserchatService;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.ApiWhatsappService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/v1/whatsapp")
public class WhatsappController {

    private final ApiWhatsappService apiWhatsappService;
    private final UserchatService userchatService;

    public WhatsappController(ApiWhatsappService apiWhatsappService, UserchatService userchatService) {
        this.apiWhatsappService = apiWhatsappService;
        this.userchatService = userchatService;
    }

    @Autowired
    private ApiKeyFilter apiKeyFilter;

    
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
            apiKeyFilter.getPhoneNumber(message.entry().get(0).changes().get(0).value().contacts().get(0).wa_id());
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
                return ResponseEntity.ok("Imagen subida con éxito. Media ID: " + mediaId);
            } else {
                return ResponseEntity.status(500).body("Error al subir la imagen.");
            }

        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error al procesar el archivo: " + e.getMessage());
        }
    }

    // ======================================================
    //   Encontrar usuario por cedula o telefono
    // ======================================================
    @GetMapping("/user/find")
    public UserChatEntity findUser(
            @RequestParam(value = "cedula", required = false) String cedula,
            @RequestParam(value = "telefono", required = false) String telefono) {

        if (cedula != null) {
            UserChatEntity dto = userchatService.findByCedula(cedula);
            return dto;
        }

        if (telefono != null) {
            UserChatEntity dto = userchatService.findByPhone(telefono);
            return dto;
        }
        
        return null;
    }

    // ======================================================
    //   Paginar todos los usuarios
    // ======================================================
    /**
     * Listado paginado de usuarios junto con sus sesiones de chat.
     *
     * Ejemplos de llamada:
     * GET /api/v1/whatsapp/page/users/0
     * GET /api/v1/whatsapp/page/users/0?pageSize=5
     * GET /api/v1/whatsapp/page/users/0?pageSize=5&sortBy=phone&direction=desc
     *
     * @param page      número de página (0-based)
     * @param pageSize  tamaño de cada página
     * @param sortBy    campo por el que ordenar
     * @param direction dirección de orden (asc o desc)
     * @return página de UserChatEntity con metadatos de paginación
     */
    @GetMapping("/page/users/{page}")
    public ResponseEntity<Page<UserChatEntity>> listUsers(
            @PathVariable("page") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "sortBy", defaultValue = "nombres") String sortBy,
            @RequestParam(value = "direction", defaultValue = "asc") String direction) {

        Page<UserChatEntity> usersPage = userchatService.findAll(page, pageSize, sortBy, direction);
        return ResponseEntity.ok(usersPage);
    }
}
