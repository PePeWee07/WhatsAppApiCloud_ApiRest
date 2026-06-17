package com.BackEnd.WhatsappApiCloud.model.dto.openIA.promptConfig;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/** Representación de la config de prompt para el controlador (lectura). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiPromptConfigDto(
        Long id,
        String name,
        boolean active,
        int version,
        String instructions,
        String model,
        Double temperature,
        Double topP,
        Integer maxOutputTokens,
        JsonNode reasoning,
        JsonNode tools,
        JsonNode include,
        JsonNode text,
        boolean store,
        Instant updatedAt) {
}
