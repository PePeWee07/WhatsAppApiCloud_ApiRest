package com.BackEnd.WhatsappApiCloud.service.openAi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.BackEnd.WhatsappApiCloud.exception.ApiInfoException;
import com.BackEnd.WhatsappApiCloud.exception.ServerClientException;
import com.BackEnd.WhatsappApiCloud.model.dto.openIA.AnswersOpenIADto;
import com.BackEnd.WhatsappApiCloud.model.dto.openIA.QuestionOpenIADto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;


@Component
public class openAiServerClient {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RestClient restClient;
    private final String uriOpenAiServer;

    public openAiServerClient(
       @Value("${baseurl.ai.service}") String baseUrlOpenAiServer,
       @Value("${uri.ai.service}") String uriOpenAiServer,
       @Value("${ai.service.api.key}") String apiKeyOpenAI
    ) {
        this.uriOpenAiServer = uriOpenAiServer;
        this.restClient = RestClient.builder()
            .baseUrl(baseUrlOpenAiServer)
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Authorization", "Bearer " + apiKeyOpenAI)
            .build();
    }

    /**
     * Llama a GET {baseUrlOpenAiServer}{uriOpenAiServer} para obtener datos del servidor OpenAI.
     * 
     * @return Respuesta del servidor OpenAI.
     */

    public AnswersOpenIADto getOpenAiData(QuestionOpenIADto question) throws JsonMappingException, JsonProcessingException {
        try {
            AnswersOpenIADto answer = restClient.post()
                .uri(uriOpenAiServer)
                .body(question)
                .header("Content-Type", "application/json")
                .retrieve()
                .body(AnswersOpenIADto.class);
    
            return answer;

        }  catch (HttpClientErrorException.BadRequest e) {
            String responseBody = e.getResponseBodyAsString();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            if (rootNode.has("info")) {
                String infoMessage = rootNode.get("info").asText();
                    if (rootNode.has("moderation")) {
                        String moderationValue = rootNode.get("moderation").asText();
                        throw new ApiInfoException(infoMessage, moderationValue);
                    }
                throw new ApiInfoException(infoMessage, null);
            } else {
                logger.error("Bad Request al obtener respuesta de IA: " + e);
                throw new RuntimeException(e);
            }
        } catch (RestClientException e) {
            logger.error("Error al obtener respuesta del Asistente virtual: ", e);
            throw new ServerClientException("Error al obtener respuesta del Asistente virtual: ", e);
        }
    }
}
