package com.BackEnd.WhatsappApiCloud.service.openAi.impl;

import com.BackEnd.WhatsappApiCloud.model.dto.openIA.AnswersOpenIADto;
import com.BackEnd.WhatsappApiCloud.model.dto.openIA.DataResponse.DataHistoryDto;
import com.BackEnd.WhatsappApiCloud.model.entity.openIA.AiToolCallEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.openIA.AiResponseEntity;
import com.BackEnd.WhatsappApiCloud.repository.AitoolCallRepository;
import com.BackEnd.WhatsappApiCloud.service.openAi.AiResponseService;
import com.BackEnd.WhatsappApiCloud.repository.AiResponseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiResponseServiceImpl implements AiResponseService {

    private final AiResponseRepository aiResponseRepo;
    private final AitoolCallRepository toolRepo;
    private final ObjectMapper objectMapper;

    public AiResponseServiceImpl(
            AiResponseRepository aiResponseRepo,
            AitoolCallRepository toolRepo,
            ObjectMapper objectMapper) {
        this.aiResponseRepo = aiResponseRepo;
        this.toolRepo = toolRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void saveAiResponses(AnswersOpenIADto payload, MessageEntity message) throws JsonProcessingException {

        for (DataHistoryDto dto : payload.data()) {

            // 1) Crear AiResponseEntity (un registro por cada dto)
            AiResponseEntity ai = new AiResponseEntity();
            ai.setResponseId(dto.response_id());
            ai.setPreviousResponseId(dto.previous_response_id());
            ai.setCreatedAt(Instant.ofEpochSecond(dto.created_at()));
            ai.setModel(dto.model());
            ai.setPromptId(dto.prompt().id());
            ai.setPromptVariables(objectMapper.writeValueAsString(dto.prompt().variables()));
            ai.setPromptVersion(dto.prompt().version());
            ai.setInputTokens(dto.usage().input_tokens());
            ai.setOutputTokens(dto.usage().output_tokens());
            ai.setTotalTokens(dto.usage().total_tokens());
            ai.setMetadata(objectMapper.writeValueAsString(dto.metadata()));
            ai.setReasoning(objectMapper.writeValueAsString(dto.reasoning()));
            ai.setMessage(message);

            AiResponseEntity savedAi = aiResponseRepo.save(ai);

            // 2) Tool calls (si existen)
            Map<String, AiToolCallEntity> calls = new HashMap<>();

            for (Map<String, Object> blk : dto.input()) {
                String type = (String) blk.get("type");

                if ("function_call".equals(type)) {
                    String callId = (String) blk.get("call_id");

                    AiToolCallEntity call = new AiToolCallEntity();
                    call.setAiResponse(savedAi);
                    call.setCallId(callId);
                    call.setToolName((String) blk.get("name"));
                    call.setArguments(objectMapper.writeValueAsString(blk.get("arguments")));

                    calls.put(callId, call);

                } else if ("function_call_output".equals(type)) {
                    String callId = (String) blk.get("call_id");
                    AiToolCallEntity call = calls.get(callId);
                    if (call != null) {
                        call.setOutput((String) blk.get("output"));
                    }
                }
            }

            for (AiToolCallEntity call : calls.values()) {
                toolRepo.save(call);
            }
        }
    }
}
