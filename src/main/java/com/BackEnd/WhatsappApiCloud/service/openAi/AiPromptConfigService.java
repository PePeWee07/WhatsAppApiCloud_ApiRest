package com.BackEnd.WhatsappApiCloud.service.openAi;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.BackEnd.WhatsappApiCloud.exception.BadRequestException;
import com.BackEnd.WhatsappApiCloud.model.dto.openIA.promptConfig.AiPromptConfigDto;
import com.BackEnd.WhatsappApiCloud.model.dto.openIA.promptConfig.AiPromptConfigRequest;
import com.BackEnd.WhatsappApiCloud.model.dto.openIA.promptConfig.PromptConfigSnapshot;
import com.BackEnd.WhatsappApiCloud.model.dto.openIA.promptConfig.ResolvedPromptConfigDto;
import com.BackEnd.WhatsappApiCloud.model.entity.openIA.AiPromptConfigEntity;
import com.BackEnd.WhatsappApiCloud.repository.AiPromptConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Capa de configuración del prompt de CatIA. Permite editar instrucciones, modelo, parámetros,
 * tools y text EN CALIENTE.
 *
 * - Hot path: resolveFor(Map) toma la instantánea cacheada (via AiPromptConfigCache),
 *   interpola las variables del usuario en las instrucciones y parsea los JSON a nodos reales.
 * - CRUD: lectura/edición de la config activa (evicta la caché en cada cambio).
 * - Seed: ensureDefault() crea una config genérica si no existe ninguna.
 */
@Service
public class AiPromptConfigService {

    public static final String CACHE_NAME = "aiPromptConfigCache";
    public static final String DEFAULT_NAME = "default";

    private static final Logger logger = LoggerFactory.getLogger(AiPromptConfigService.class);

    private final AiPromptConfigRepository repo;
    private final AiPromptConfigCache cache;
    private final ObjectMapper objectMapper;

    public AiPromptConfigService(AiPromptConfigRepository repo, AiPromptConfigCache cache, ObjectMapper objectMapper) {
        this.repo = repo;
        this.cache = cache;
        this.objectMapper = objectMapper;
    }

    // ============== Hot path (lo usa ApiWhatsappServiceImpl en cada mensaje) ==============

    /**
     * Devuelve la config lista para Gpt-Tics: interpola las variables del usuario en las
     * instrucciones y parsea reasoning/tools/include/text a JSON real. Lee desde caché (cross-bean).
     */
    public ResolvedPromptConfigDto resolveFor(Map<String, String> variables) {
        PromptConfigSnapshot s = cache.getActiveSnapshot();
        String instructions = interpolate(s.instructions(), variables);
        return new ResolvedPromptConfigDto(
                instructions,
                s.model(),
                s.temperature(),
                s.topP(),
                s.maxOutputTokens(),
                parse(s.reasoningJson()),
                parse(s.toolsJson()),
                parse(s.includeJson()),
                s.store(),
                parse(s.textJson()));
    }

    private static String interpolate(String template, Map<String, String> vars) {
        if (template == null) {
            return "";
        }
        String out = template;
        if (vars != null) {
            for (Map.Entry<String, String> v : vars.entrySet()) {
                String value = (v.getValue() == null || v.getValue().isBlank()) ? "unknown" : v.getValue();
                out = out.replace("{{" + v.getKey() + "}}", value);
            }
        }
        return out;
    }

    private JsonNode parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new IllegalStateException("JSON inválido en la configuración del prompt: " + ex.getMessage(), ex);
        }
    }

    // ============================== CRUD (controlador) ==============================

    @Transactional(readOnly = true)
    public AiPromptConfigDto getActive() {
        return toDto(requireActive());
    }

    /** Edición parcial: solo los campos no nulos del request. Sube versión y evicta la caché. */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    @Transactional
    public AiPromptConfigDto updateActive(AiPromptConfigRequest req) {
        AiPromptConfigEntity e = requireActive();
        if (req.instructions() != null) {
            e.setInstructions(req.instructions());
        }
        if (req.model() != null) {
            e.setModel(req.model());
        }
        if (req.temperature() != null) {
            e.setTemperature(req.temperature());
        }
        if (req.topP() != null) {
            e.setTopP(req.topP());
        }
        if (req.maxOutputTokens() != null) {
            e.setMaxOutputTokens(req.maxOutputTokens());
        }
        if (req.reasoning() != null) {
            e.setReasoningJson(req.reasoning().toString());
        }
        if (req.tools() != null) {
            e.setToolsJson(req.tools().toString());
        }
        if (req.include() != null) {
            e.setIncludeJson(req.include().toString());
        }
        if (req.text() != null) {
            e.setTextJson(req.text().toString());
        }
        if (req.store() != null) {
            e.setStore(req.store());
        }
        // Limpia explícitamente los campos pedidos (null = "no tocar", por eso hace falta 'clear').
        applyClears(e, req.clear());
        // Rechaza combinaciones imposibles antes de guardar (p. ej. reasoning + temperature).
        validateCoherence(e);
        e.setVersion(e.getVersion() + 1);
        e.setUpdatedAt(Instant.now());
        return toDto(repo.save(e));
    }

    /** Pone en null los campos indicados. Solo se permiten los que un modelo podría no soportar. */
    private void applyClears(AiPromptConfigEntity e, List<String> clear) {
        if (clear == null) {
            return;
        }
        for (String field : clear) {
            if (field == null || field.isBlank()) {
                continue;
            }
            switch (field.trim().toLowerCase()) {
                case "temperature" -> e.setTemperature(null);
                case "top_p", "topp" -> e.setTopP(null);
                case "max_output_tokens", "maxoutputtokens" -> e.setMaxOutputTokens(null);
                case "reasoning" -> e.setReasoningJson(null);
                case "include" -> e.setIncludeJson(null);
                case "text" -> e.setTextJson(null);
                default -> throw new BadRequestException(
                        "Campo no permitido en 'clear': '" + field
                        + "'. Permitidos: temperature, top_p, max_output_tokens, reasoning, include, text.");
            }
        }
    }

    /**
     * Rechaza combinaciones imposibles: 'reasoning' y 'temperature'/'top_p' no pueden coexistir
     */
    private void validateCoherence(AiPromptConfigEntity e) {
        String r = e.getReasoningJson();
        boolean hasReasoning = r != null && !r.isBlank()
                && !r.trim().equals("null") && !r.trim().equals("{}");
        boolean hasSampling = e.getTemperature() != null || e.getTopP() != null;
        if (hasReasoning && hasSampling) {
            throw new BadRequestException(
                    "Configuración incompatible: 'reasoning' no puede ir junto con 'temperature'/'top_p'. "
                    + "Usa 'clear' para quitar los que no apliquen al modelo elegido.");
        }
    }

    private AiPromptConfigEntity requireActive() {
        return repo.findByActiveTrue()
                .orElseThrow(() -> new BadRequestException("No hay configuración de prompt activa."));
    }

    // ================================ Seed ================================

    /** Crea la config por defecto (prompt genérico) si todavía no existe ninguna activa. Idempotente. */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    @Transactional
    public void ensureDefault() {
        if (repo.findByActiveTrue().isPresent()) {
            return;
        }
        AiPromptConfigEntity e = repo.findByName(DEFAULT_NAME).orElseGet(AiPromptConfigEntity::new);
        e.setName(DEFAULT_NAME);
        e.setActive(true);
        e.setVersion(1);
        e.setInstructions(DEFAULT_INSTRUCTIONS);
        e.setModel(DEFAULT_MODEL);
        // El modelo por defecto es de razonamiento (gpt-5.x): NO acepta temperature/top_p, así que
        // van en null. Si cambias a un modelo clásico, setéalos y limpia 'reasoning' vía el endpoint.
        e.setTemperature(null);
        e.setTopP(null);
        e.setMaxOutputTokens(null); // sin límite
        e.setReasoningJson(DEFAULT_REASONING_JSON);
        e.setToolsJson(loadResource("ai/default-tools.json", "[]"));
        e.setIncludeJson(DEFAULT_INCLUDE_JSON);
        e.setTextJson(DEFAULT_TEXT_JSON);
        e.setStore(true);
        e.setUpdatedAt(Instant.now());
        repo.save(e);
        logger.info("Config de prompt por defecto creada (name='{}').", DEFAULT_NAME);
    }

    private String loadResource(String classpath, String fallback) {
        try (InputStream is = new ClassPathResource(classpath).getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            logger.warn("No se pudo cargar el recurso '{}'; uso fallback. Causa: {}", classpath, ex.getMessage());
            return fallback;
        }
    }

    private AiPromptConfigDto toDto(AiPromptConfigEntity e) {
        return new AiPromptConfigDto(
                e.getId(),
                e.getName(),
                e.isActive(),
                e.getVersion(),
                e.getInstructions(),
                e.getModel(),
                e.getTemperature(),
                e.getTopP(),
                e.getMaxOutputTokens(),
                parse(e.getReasoningJson()),
                parse(e.getToolsJson()),
                parse(e.getIncludeJson()),
                parse(e.getTextJson()),
                e.isStore(),
                e.getUpdatedAt());
    }

    // ===================== Defaults (prompt genérico; reemplázalo luego) =====================

    private static final String DEFAULT_MODEL = "gpt-5.4-mini-2026-03-17";

    private static final String DEFAULT_REASONING_JSON = "{\"summary\":\"auto\"}";

    private static final String DEFAULT_INCLUDE_JSON =
            "[\"reasoning.encrypted_content\",\"web_search_call.action.sources\"]";

    private static final String DEFAULT_TEXT_JSON =
            "{\"format\":{\"type\":\"text\"},\"verbosity\":\"medium\"}";

    private static final String DEFAULT_INSTRUCTIONS = """
            Eres CatIA, el asistente virtual de soporte TIC de la Universidad Católica de Cuenca (UCACUE).
            Atiendes por WhatsApp de forma clara, breve y amable.

            Datos del usuario con el que conversas:
            - Nombres: {{names}}
            - Teléfono: {{phone}}
            - Roles: {{roles}}
            - Identificación: {{identificacion}}
            - Email institucional: {{email_institucional}}
            - Email personal: {{email_personal}}
            - Sexo: {{sexo}}

            Ayuda al usuario con sus solicitudes de soporte TIC y usa las herramientas disponibles
            cuando corresponda. (Prompt genérico de ejemplo: reemplázalo por el definitivo vía el
            endpoint PUT /api/v1/ai-prompt-config.)
            """;
}
