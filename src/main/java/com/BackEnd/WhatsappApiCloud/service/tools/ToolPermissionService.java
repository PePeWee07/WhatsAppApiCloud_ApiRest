package com.BackEnd.WhatsappApiCloud.service.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.BackEnd.WhatsappApiCloud.exception.BadRequestException;
import com.BackEnd.WhatsappApiCloud.model.dto.tool.ToolPermissionDto;
import com.BackEnd.WhatsappApiCloud.model.entity.tool.ToolPermissionEntity;
import com.BackEnd.WhatsappApiCloud.repository.ToolPermissionRepository;

@Service
public class ToolPermissionService {

    public static final String CACHE_NAME = "toolPermissionsCache";

    // Roles "de fábrica" para las tools básicas.
    private static final Set<String> DEFAULT_ROLES = Set.of("DOCENTE", "ADMINISTRATIVO", "ENCARGATURA");

    // Tools básicas conocidas por el core (deben coincidir con los nombres registrados en Gpt-Tics).
    private static final List<String> DEFAULT_TOOLS = List.of(
        "send_support_email",
        "invite_user_feedback",
        "get_user_tickets",
        "submit_support_case",
        "get_ticket_info",
        "agg_attachment_existing_ticket",
        "accept_ticket",
        "open_attachment_session",
        "create_ticket_note",
        "request_human_handoff"
    );

    private final ToolPermissionRepository repo;

    public ToolPermissionService(ToolPermissionRepository repo) {
        this.repo = repo;
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }

    /**
     * Mapa toolName -> roles permitidos (en MAYÚSCULAS) que se envía a Gpt-Tics en cada /ask.
     * Una tool deshabilitada (enabled=false) se devuelve con lista vacía => se deniega a todos.
     * Las tools que NO estén en esta tabla no aparecen aquí: Gpt-Tics usará su default estático.
     */
    @Cacheable(CACHE_NAME)
    @Transactional(readOnly = true)
    public Map<String, List<String>> getPermissionsMap() {
        Map<String, List<String>> map = new HashMap<>();
        for (ToolPermissionEntity e : repo.findAll()) {
            List<String> roles = e.isEnabled()
                    ? e.getAllowedRoles().stream()
                        .map(ToolPermissionService::norm)
                        .filter(r -> !r.isEmpty())
                        .distinct()
                        .collect(Collectors.toList())
                    : new ArrayList<>(); // deshabilitada -> denegar a todos
            map.put(e.getToolName(), roles);
        }
        return map;
    }

    @Transactional(readOnly = true)
    public List<ToolPermissionDto> listAll() {
        return repo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    /** Crea o actualiza los roles (y opcionalmente el estado enabled) de una tool. */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    @Transactional
    public ToolPermissionDto upsert(String toolName, Set<String> roles, Boolean enabled) {
        if (toolName == null || toolName.isBlank()) {
            throw new BadRequestException("toolName es obligatorio.");
        }
        ToolPermissionEntity e = repo.findByToolName(toolName.trim())
                .orElseGet(() -> {
                    ToolPermissionEntity n = new ToolPermissionEntity();
                    n.setToolName(toolName.trim());
                    n.setEnabled(true);
                    return n;
                });

        if (roles != null) {
            e.setAllowedRoles(roles.stream()
                    .map(ToolPermissionService::norm)
                    .filter(r -> !r.isEmpty())
                    .collect(Collectors.toSet()));
        }
        if (enabled != null) {
            e.setEnabled(enabled);
        }
        return toDto(repo.save(e));
    }

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    @Transactional
    public ToolPermissionDto setEnabled(String toolName, boolean enabled) {
        ToolPermissionEntity e = repo.findByToolName(toolName)
                .orElseThrow(() -> new BadRequestException("Tool no encontrada: " + toolName));
        e.setEnabled(enabled);
        return toDto(repo.save(e));
    }

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    @Transactional
    public void delete(String toolName) {
        repo.findByToolName(toolName).ifPresent(repo::delete);
    }

    /**
     * Restaura los permisos "de fábrica": vuelve las tools básicas a sus roles por defecto
     * y enabled=true, SOBRESCRIBIENDO cualquier edición previa. Atómico (@Transactional) y
     * evicta la caché para reflejarse al instante. Las tools no básicas no se tocan.
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    @Transactional
    public List<ToolPermissionDto> restoreDefaults() {
        List<ToolPermissionDto> result = new ArrayList<>();
        for (String toolName : DEFAULT_TOOLS) {
            ToolPermissionEntity e = repo.findByToolName(toolName)
                    .orElseGet(() -> {
                        ToolPermissionEntity n = new ToolPermissionEntity();
                        n.setToolName(toolName);
                        return n;
                    });
            e.setAllowedRoles(new HashSet<>(DEFAULT_ROLES));
            e.setEnabled(true);
            result.add(toDto(repo.save(e)));
        }
        return result;
    }

    private ToolPermissionDto toDto(ToolPermissionEntity e) {
        return new ToolPermissionDto(e.getId(), e.getToolName(), e.getAllowedRoles(), e.isEnabled());
    }
}
