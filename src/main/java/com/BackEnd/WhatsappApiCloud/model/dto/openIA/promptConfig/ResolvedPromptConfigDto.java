package com.BackEnd.WhatsappApiCloud.model.dto.openIA.promptConfig;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Config ya resuelta (instructions interpoladas) que viaja a Gpt-Tics dentro de QuestionOpenIADto.
 * Las claves se serializan como las espera la API Responses de OpenAI (top_p, max_output_tokens,
 * text). Los campos nulos se omiten para no pisar defaults.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResolvedPromptConfigDto(
        String instructions,
        String model,
        Double temperature,
        @JsonProperty("top_p") Double topP,
        @JsonProperty("max_output_tokens") Integer maxOutputTokens,
        JsonNode reasoning,
        JsonNode tools,
        JsonNode include,
        boolean store,
        JsonNode text) {
}
