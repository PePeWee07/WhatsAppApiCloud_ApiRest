package com.BackEnd.WhatsappApiCloud.model.dto.openIA.promptConfig;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Cuerpo para editar la config activa. Edición PARCIAL: solo se aplican los campos no nulos.
 * Para poner un campo en null (p. ej. al pasar a un modelo de razonamiento que no acepta
 * temperature/top_p), inclúyelo en 'clear' — enviar null significa "no tocar".
 * Campos permitidos en 'clear': temperature, top_p, max_output_tokens, reasoning, include, text.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiPromptConfigRequest(
        String instructions,
        String model,
        Double temperature,
        Double topP,
        Integer maxOutputTokens,
        JsonNode reasoning,
        JsonNode tools,
        JsonNode include,
        JsonNode text,
        Boolean store,
        List<String> clear) {
}
