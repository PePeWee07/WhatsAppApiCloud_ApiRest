package com.BackEnd.WhatsappApiCloud.model.dto.openIA.promptConfig;

/**
 * Instantánea cacheable de la config activa. Guarda los JSON (reasoning/tools/include/text) como
 * TEXTO sin parsear, para que se serialice/deserialice en Redis sin problemas. El parseo a JsonNode
 * se hace al resolver la config por usuario (ver AiPromptConfigService#resolveFor).
 */
public record PromptConfigSnapshot(
        String instructions,
        String model,
        Double temperature,
        Double topP,
        Integer maxOutputTokens,
        String reasoningJson,
        String toolsJson,
        String includeJson,
        boolean store,
        String textJson) {
}
