package com.BackEnd.WhatsappApiCloud.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.BackEnd.WhatsappApiCloud.exception.BadRequestException;
import com.BackEnd.WhatsappApiCloud.model.dto.user.UserChatFullDto;
import com.BackEnd.WhatsappApiCloud.model.dto.user.UserTicketDto;
import com.BackEnd.WhatsappApiCloud.service.userChat.UserchatService;
import com.BackEnd.WhatsappApiCloud.util.UserChatFieldsSorby;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/v1/whatsapp")
public class UserChatController {

    private final UserchatService userchatService;
    private static final int MAX_PAGE_SIZE = 100;

    public UserChatController(UserchatService userchatService) {
        this.userchatService = userchatService;
    }

    // ========== Encontrar usuario por identificacion o whatsappPhone =============
    @GetMapping("/user/find")
    public ResponseEntity<UserChatFullDto> findUser(
        @RequestParam(value = "identificacion", required = false) String identificacion,
        @RequestParam(value = "whatsappPhone", required = false) String whatsAppPhone) {

        if (identificacion != null && whatsAppPhone != null) {
            throw new BadRequestException("Debe indicar solo un paramentro de busqueda: identificacion o whatsappPhone");
        }

        if (identificacion == null && whatsAppPhone == null) {
            throw new BadRequestException("Debe indicar un paramentro de busqueda: identificacion o whatsappPhone");
        }

        UserChatFullDto dto;
        if (identificacion != null) {
            dto = userchatService.findByIdentificacion(identificacion);
        } else {
            dto = userchatService.findByWhatsappPhone(whatsAppPhone);
        }
        return ResponseEntity.ok(dto);
    }

    // =================== Paginar todos los usuarios ========================
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
    public ResponseEntity<Page<UserChatFullDto>> listUsers(
            @PathVariable("page") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "sortBy", defaultValue = "lastInteraction") String sortBy,
            @RequestParam(value = "direction", defaultValue = "asc") String direction) {

        int size = Math.min(pageSize, MAX_PAGE_SIZE);

        if (!UserChatFieldsSorby.ALLOWED_SORT_FIELDS.contains(sortBy)) {
            return ResponseEntity.badRequest().body(Page.empty());
        }

        Page<UserChatFullDto> usersPage = userchatService.usersTable(page, size, sortBy, direction);
        return ResponseEntity.ok(usersPage);
    }

    // ================== Paginar usuarios por fecha de última interacción ========================
    @GetMapping("/page/users/{page}/byLastInteraction")
    public ResponseEntity<Page<UserChatFullDto>> listByLastInteraction(
            @PathVariable("page") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "sortBy",   defaultValue = "lastInteraction") String sortBy,
            @RequestParam(value = "direction", defaultValue = "asc")     String direction,
            @RequestParam("startDate") String startDateStr,
            @RequestParam("endDate")   String endDateStr) {
        
        int size = Math.min(pageSize, MAX_PAGE_SIZE);

        if (!UserChatFieldsSorby.ALLOWED_SORT_FIELDS.contains(sortBy)) {
            return ResponseEntity.badRequest().body(Page.empty());
        }
        
        LocalDateTime inicio, fin;
        inicio = LocalDateTime.parse(startDateStr, DateTimeFormatter.ISO_DATE_TIME);
        fin    = LocalDateTime.parse(endDateStr,   DateTimeFormatter.ISO_DATE_TIME);

        if (inicio.isAfter(fin)) {
            return ResponseEntity.badRequest().build();
        }

        Page<UserChatFullDto> usuarios = userchatService.tablefindByLastInteraction(page, size, sortBy, direction, inicio, fin);
        return ResponseEntity.ok(usuarios);
    }

    // ================== Paginar usuarios por fecha de inicio de sesión de chat ========================
    @GetMapping("/page/users/{page}/byChatSessionStart")
    public ResponseEntity<Page<UserChatFullDto>> listChatSessionStart(
            @PathVariable("page") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "sortBy",   defaultValue = "lastInteraction") String sortBy,
            @RequestParam(value = "direction",defaultValue = "asc")     String direction,
            @RequestParam("startDate") String startDateStr,
            @RequestParam("endDate")   String endDateStr) {
        
        int size = Math.min(pageSize, MAX_PAGE_SIZE);

        if (!UserChatFieldsSorby.ALLOWED_SORT_FIELDS.contains(sortBy)) {
            return ResponseEntity.badRequest().body(Page.empty());
        }
        
        LocalDateTime inicio, fin;
        inicio = LocalDateTime.parse(startDateStr, DateTimeFormatter.ISO_DATE_TIME);
        fin    = LocalDateTime.parse(endDateStr,   DateTimeFormatter.ISO_DATE_TIME);

        if (inicio.isAfter(fin)) {
            return ResponseEntity.badRequest().build();
        }

        Page<UserChatFullDto> usuarios = userchatService.tablefindByChatSessionStart(page, size, sortBy, direction, inicio, fin);
        return ResponseEntity.ok(usuarios);
    }

    // ================== Actualizar datos de un usuario =======================
    @PatchMapping("/update/user/{id}")
    public ResponseEntity<UserChatFullDto> patchUser(
            @PathVariable("id") Long id,
            @RequestBody Map<String, Object> updates) {

        UserChatFullDto patched = userchatService.userUpdate(id, updates);
        return ResponseEntity.ok(patched);
    }

    // ================== Solicitar información de un ticket ==================
    @GetMapping("/user/ticket/info")
    public ResponseEntity<String> getTicketInfo(
            @RequestParam("whatsappPhone") String whatsAppPhone,
            @RequestParam("ticketId") String ticketId) throws IOException {
            userchatService.userRequestTicketInfo(whatsAppPhone, ticketId);
            return ResponseEntity.ok("La Información del ticket fue enviada correctamente por WhatsApp.");
        
    }

    // ================== Enviar lista de tickets a WhatsApp ==================
    @GetMapping("/user/tickets/open")
    public ResponseEntity<List<UserTicketDto>> sendTicketListToWhatsApp(
            @RequestParam("whatsappPhone") String whatsAppPhone) throws JsonProcessingException {
            List<UserTicketDto> ticketList = userchatService.userRequestTicketList(whatsAppPhone);
            return ResponseEntity.ok(ticketList);
    }

}
