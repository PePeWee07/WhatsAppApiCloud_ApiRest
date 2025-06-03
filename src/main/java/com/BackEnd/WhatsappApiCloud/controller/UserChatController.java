package com.BackEnd.WhatsappApiCloud.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.BackEnd.WhatsappApiCloud.model.dto.user.UserChatFullDto;
import com.BackEnd.WhatsappApiCloud.service.userChat.UserchatService;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/v1/whatsapp")
public class UserChatController {

    private final UserchatService userchatService;

    public UserChatController(UserchatService userchatService) {
        this.userchatService = userchatService;
    }

    // ========== Encontrar usuario por identificacion o whatsappPhone =============
    @GetMapping("/user/find")
    public UserChatFullDto findUser(
            @RequestParam(value = "identificacion", required = false) String identificacion,
            @RequestParam(value = "whatsappPhone", required = false) String whatsappPhone) {

        if (identificacion != null) {
            UserChatFullDto dto = userchatService.findByIdentificacion(identificacion);
            return dto;
        }

        if (whatsappPhone != null) {
            UserChatFullDto dto = userchatService.findByWhatsappPhone(whatsappPhone);
            return dto;
        }
        
        return null;
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
            @RequestParam(value = "sortBy", defaultValue = "nombres") String sortBy,
            @RequestParam(value = "direction", defaultValue = "asc") String direction) {

        Page<UserChatFullDto> usersPage = userchatService.findAll(page, pageSize, sortBy, direction);
        return ResponseEntity.ok(usersPage);
    }

    // ================== Actualizar datos de un usuario =======================
    @PatchMapping("/update/user/{id}")
    public ResponseEntity<UserChatFullDto> patchUser(
            @PathVariable("id") Long id,
            @RequestBody Map<String, Object> updates) {

        UserChatFullDto patched = userchatService.patchUser(id, updates);
        return ResponseEntity.ok(patched);
    }

}
