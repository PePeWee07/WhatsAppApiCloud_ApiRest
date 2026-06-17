package com.BackEnd.WhatsappApiCloud.service.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.BackEnd.WhatsappApiCloud.exception.BadRequestException;
import com.BackEnd.WhatsappApiCloud.model.dto.tool.ToolPermissionDto;
import com.BackEnd.WhatsappApiCloud.model.entity.openIA.AiPromptConfigEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.tool.ToolPermissionEntity;
import com.BackEnd.WhatsappApiCloud.repository.AiPromptConfigRepository;
import com.BackEnd.WhatsappApiCloud.repository.ToolPermissionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ToolPermissionService {

    public static final String CACHE_NAME = "toolPermissionsCache";

    private static final Logger logger = LoggerFactory.getLogger(ToolPermissionService.class);

    // Roles "de fábrica" que aplica restoreDefaults() (política por defecto, NO una lista de tools).
    private static final Set<String> DEFAULT_ROLES = Set.of("DOCENTE", "ADMINISTRATIVO", "ENCARGATURA");

    private final ToolPermissionRepository repo;
    private final AiPromptConfigRepository promptConfigRepo;
    private final ObjectMapper objectMapper;

    public ToolPermissionService(ToolPermissionRepository repo,
                                 AiPromptConfigRepository promptConfigRepo,
                                 ObjectMapper objectMapper) {
        this.repo = repo;
        this.promptConfigRepo = promptConfigRepo;
        this.objectMapper = objectMapper;
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }

    /**
     * Lee los nombres de las tools de tipo "function" definidas en la config activa
     * (ai_prompt_config.tools_json). Las tools hosted (file_search, web_search, ...) se ignoran:
     * no tienen 'name' ni permisos por rol.
     */
    private List<String> functionToolNamesFromActiveConfig() {
        AiPromptConfigEntity cfg = promptConfigRepo.findByActiveTrue()
                .orElseThrow(() -> new BadRequestException(
                        "No hay configuración de prompt activa; no se puede sincronizar la lista de tools."));
        String toolsJson = cfg.getToolsJson();
        if (toolsJson == null || toolsJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            JsonNode arr = objectMapper.readTree(toolsJson);
            List<String> names = new ArrayList<>();
            if (arr.isArray()) {
                for (JsonNode t : arr) {
                    JsonNode type = t.get("type");
                    JsonNode name = t.get("name");
                    if (type != null && "function".equals(type.asText())
                            && name != null && !name.asText().isBlank()) {
                        names.add(name.asText().trim());
                    }
                }
            }
            return names;
        } catch (Exception ex) {
            throw new IllegalStateException("tools_json inválido en la configuración del prompt: " + ex.getMessage(), ex);
        }
    }

    /**
     * Mapa toolName -> roles permitidos (en MAYÚSCULAS) que se envía a Gpt-Tics en cada /ask.
     * Una tool deshabilitada (enabled=false) se devuelve con lista vacía => se deniega a todos.
     * Las tools que NO estén en esta tabla no aparecen aquí: Gpt-Tics usará su default estático.
     */
    @Cacheable(value = CACHE_NAME, key = "'roles'")
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

    /**
     * Mapa toolName -> segundos de cooldown configurados. Solo incluye tools con cooldown > 0
     * (las ausentes se tratan como sin enfriamiento).
     */
    @Cacheable(value = CACHE_NAME, key = "'cooldowns'")
    @Transactional(readOnly = true)
    public Map<String, Integer> getCooldownMap() {
        Map<String, Integer> map = new HashMap<>();
        for (ToolPermissionEntity e : repo.findAll()) {
            int cd = e.getCooldownSeconds() == null ? 0 : e.getCooldownSeconds();
            if (cd > 0) {
                map.put(e.getToolName(), cd);
            }
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
    public ToolPermissionDto upsert(String toolName, Set<String> roles, Boolean enabled, Integer cooldownSeconds) {
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
        if (cooldownSeconds != null) {
            e.setCooldownSeconds(Math.max(0, cooldownSeconds));
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
     * Sincroniza la tabla de permisos con las function tools de la config activa:
     * - crea las que falten DESHABILITADAS y sin roles (opt-in explícito: se habilitan luego);
     * - respeta las que ya existen (no toca sus roles/cooldown);
     * - NO borra nada: solo registra en el log las tools de la tabla que ya no están en la config.
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    @Transactional
    public List<ToolPermissionDto> syncFromConfig() {
        List<String> configTools = functionToolNamesFromActiveConfig();
        Set<String> configSet = new HashSet<>(configTools);

        List<ToolPermissionDto> result = new ArrayList<>();
        for (String toolName : configTools) {
            ToolPermissionEntity e = repo.findByToolName(toolName).orElse(null);
            if (e == null) {
                e = new ToolPermissionEntity();
                e.setToolName(toolName);
                e.setAllowedRoles(new HashSet<>()); // sin roles => denegada hasta configurar
                e.setEnabled(false);                // deshabilitada por defecto
                e.setCooldownSeconds(0);
                e = repo.save(e);
                logger.info("Tool '{}' creada en permisos (deshabilitada, sin roles) por sync.", toolName);
            }
            result.add(toDto(e));
        }

        // Sobrantes: en la tabla pero ya no en la config (no se borran, solo se avisan).
        for (ToolPermissionEntity e : repo.findAll()) {
            if (!configSet.contains(e.getToolName())) {
                logger.warn("Tool '{}' está en tool_permissions pero NO en la config activa (sobrante).",
                        e.getToolName());
            }
        }
        return result;
    }

    /**
     * Reset de fábrica de PERMISOS: a las function tools de la config activa les aplica
     * DEFAULT_ROLES y enabled=true, SOBRESCRIBIENDO ediciones previas. La LISTA de tools ya no
     * está hardcodeada: sale de la config (ai_prompt_config.tools_json).
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    @Transactional
    public List<ToolPermissionDto> restoreDefaults() {
        List<ToolPermissionDto> result = new ArrayList<>();
        for (String toolName : functionToolNamesFromActiveConfig()) {
            ToolPermissionEntity e = repo.findByToolName(toolName)
                    .orElseGet(() -> {
                        ToolPermissionEntity n = new ToolPermissionEntity();
                        n.setToolName(toolName);
                        return n;
                    });
            e.setAllowedRoles(new HashSet<>(DEFAULT_ROLES));
            e.setEnabled(true);
            e.setCooldownSeconds(0);
            result.add(toDto(repo.save(e)));
        }
        return result;
    }

    private ToolPermissionDto toDto(ToolPermissionEntity e) {
        int cd = e.getCooldownSeconds() == null ? 0 : e.getCooldownSeconds();
        return new ToolPermissionDto(e.getId(), e.getToolName(), e.getAllowedRoles(), e.isEnabled(), cd);
    }
}
