package com.BackEnd.WhatsappApiCloud.service.userChat.impl;

import com.BackEnd.WhatsappApiCloud.model.dto.openIA.AnswersOpenIADto;
import com.BackEnd.WhatsappApiCloud.model.dto.openIA.DataResponse.DataHistoryDto;
import com.BackEnd.WhatsappApiCloud.model.dto.user.chatHistory.ChatToolCallDto;
import com.BackEnd.WhatsappApiCloud.model.dto.user.chatHistory.ConversationBlockDto;
import com.BackEnd.WhatsappApiCloud.model.entity.user.ChatMessageEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.user.ChatToolCallEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.user.ChatTurnEntity;
import com.BackEnd.WhatsappApiCloud.repository.ChatMessageRepository;
import com.BackEnd.WhatsappApiCloud.repository.ChatToolCallRepository;
import com.BackEnd.WhatsappApiCloud.repository.ChatTurnRepository;
import com.BackEnd.WhatsappApiCloud.service.userChat.ChatHistoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private final ChatTurnRepository turnRepo;
    private final ChatMessageRepository msgRepo;
    private final ChatToolCallRepository toolRepo;
    private final ObjectMapper objectMapper;

    public ChatHistoryServiceImpl(
        ChatTurnRepository turnRepo,
        ChatMessageRepository msgRepo,
        ChatToolCallRepository toolRepo,
        ObjectMapper objectMapper
    ) {
        this.turnRepo     = turnRepo;
        this.msgRepo      = msgRepo;
        this.toolRepo     = toolRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void saveHistory(AnswersOpenIADto payload, String whatsappPhone) throws JsonProcessingException {
        for (DataHistoryDto dto : payload.data()) {
            // --- 1) Persistir el turno ---
            ChatTurnEntity turn = new ChatTurnEntity();
            turn.setResponseId(dto.response_id());
            turn.setPreviousResponseId(dto.previous_response_id());
            turn.setCreatedAt(Instant.ofEpochSecond(dto.created_at()));
            turn.setModel(dto.model());
            turn.setWhatsappPhone(whatsappPhone); // WhatsApp del usuario
            turn.setPromptId(dto.prompt().id());
            turn.setPromptVariables(objectMapper.writeValueAsString(dto.prompt().variables()));
            turn.setPromptVersion(dto.prompt().version());
            turn.setInputTokens(dto.usage().input_tokens());
            turn.setOutputTokens(dto.usage().output_tokens());
            turn.setTotalTokens(dto.usage().total_tokens());
            turn.setMetadata(objectMapper.writeValueAsString(dto.metadata()));
            turn.setReasoning(objectMapper.writeValueAsString(dto.reasoning()));
            ChatTurnEntity savedTurn = turnRepo.save(turn);

            // --- 2) Recorrer dto.input para mensajes y llamadas a funciones ---
            int sequence = 0;
            Map<String,ChatToolCallEntity> calls = new HashMap<>();

            for (Map<String, Object> blk : dto.input()) {
                String type = (String) blk.get("type");
                String role = (String) blk.get("role");

                // 2.a Mensaje de usuario
                if ("user".equals(role)) {
                    ChatMessageEntity msg = new ChatMessageEntity();
                    msg.setTurn(savedTurn);
                    msg.setMessageIndex(sequence++);
                    msg.setRole("user");
                    msg.setContent((String) blk.get("content"));
                    msgRepo.save(msg);

                // 2.b Llamada a función
                } else if ("function_call".equals(type)) {
                    String callId = (String) blk.get("call_id");
                    ChatToolCallEntity call = new ChatToolCallEntity();
                    call.setTurn(savedTurn);
                    call.setCallId(callId);
                    call.setToolName((String) blk.get("name"));
                    call.setArguments(objectMapper.writeValueAsString(blk.get("arguments")));
                    calls.put(callId, call);

                // 2.c Salida de función
                } else if ("function_call_output".equals(type)) {
                    String callId = (String) blk.get("call_id");
                    ChatToolCallEntity call = calls.get(callId);
                    if (call != null) {
                        call.setOutput((String) blk.get("output"));
                    }
                }
            }

            // --- 3) Mensaje final del asistente (IA) desde dto.output ---
            for (Map<String, Object> blk : dto.output()) {
                if ("message".equals(blk.get("type")) && "assistant".equals(blk.get("role"))) {
                    @SuppressWarnings("unchecked")
                    List<Map<String,Object>> contents = (List<Map<String,Object>>) blk.get("content");
                    StringBuilder text = new StringBuilder();
                    for (Map<String,Object> part : contents) {
                        if ("output_text".equals(part.get("type"))) {
                            if (text.length()>0) text.append("\n\n");
                            text.append((String) part.get("text"));
                        }
                    }
                    ChatMessageEntity ia = new ChatMessageEntity();
                    ia.setTurn(savedTurn);
                    ia.setMessageIndex(sequence++);
                    ia.setRole("assistant");
                    ia.setContent(text.toString());
                    msgRepo.save(ia);
                }
            }

            // --- 4) Persistir todas las llamadas con su output ---
            for (ChatToolCallEntity call : calls.values()) {
                toolRepo.save(call);
            }
        }
    }

    
    @Override
    @Transactional(readOnly = true)
    public List<ConversationBlockDto> getConversationBlocks(String whatsappPhone) {
        List<ChatTurnEntity> turns = turnRepo.findByWhatsappPhoneOrderByCreatedAtAsc(whatsappPhone);
        List<ConversationBlockDto> blocks = new ArrayList<>();

        String currentUserMsg = null;
        List<ChatToolCallDto> currentCalls = new ArrayList<>();

        // Variables auxiliares del turno que inicia cada bloque
        int     auxIn = 0, auxOut = 0, auxTot = 0;
        String  auxMeta = null, auxModel = null,
                auxPid = null, auxPvars = null, auxPver = null,
                auxRid = null, auxPrevRid = null,
                auxReasoning = null;
        Instant auxCreated = null;

        for (ChatTurnEntity turn : turns) {
            // 1) Buscamos primer mensaje de usuario
            ChatMessageEntity userMsgEntity = null;
            for (ChatMessageEntity m : turn.getMessages()) {
                if ("user".equals(m.getRole())) {
                    userMsgEntity = m;
                    break;
                }
            }
            if (userMsgEntity != null) {
                // Iniciar nuevo bloque
                currentUserMsg = userMsgEntity.getContent();
                currentCalls.clear();

                // Capturar todos los campos del turno
                auxIn        = turn.getInputTokens();
                auxOut       = turn.getOutputTokens();
                auxTot       = turn.getTotalTokens();
                auxMeta      = turn.getMetadata();
                auxModel     = turn.getModel();
                auxPid       = turn.getPromptId();
                auxPvars     = turn.getPromptVariables();
                auxPver      = turn.getPromptVersion();
                auxRid       = turn.getResponseId();
                auxPrevRid   = turn.getPreviousResponseId();
                auxCreated   = turn.getCreatedAt();
                auxReasoning = turn.getReasoning();
            }

            // 2) Acumular todas las llamadas a función de este turno
            for (ChatToolCallEntity call : turn.getToolCalls()) {
                currentCalls.add(new ChatToolCallDto(
                    call.getCallId(),
                    call.getToolName(),
                    call.getArguments(),
                    call.getOutput()
                ));
            }

            // 3) Buscamos primer mensaje de la IA
            ChatMessageEntity iaMsgEntity = null;
            for (ChatMessageEntity m : turn.getMessages()) {
                if ("assistant".equals(m.getRole())) {
                    iaMsgEntity = m;
                    break;
                }
            }
            
            if (iaMsgEntity != null && currentUserMsg != null) {
                // Cerramos el bloque
                blocks.add(new ConversationBlockDto(
                    // — campos del turno
                    auxIn, auxOut, auxTot,
                    auxMeta, auxModel,
                    auxPid, auxPvars, auxPver,
                    auxRid, auxPrevRid,
                    auxCreated, auxReasoning,
                    // — agrupación
                    currentUserMsg,
                    List.copyOf(currentCalls),
                    iaMsgEntity.getContent()
                ));
                // Reiniciar para el siguiente bloque
                currentUserMsg = null;
                currentCalls.clear();
            }
        }

        return blocks;
    }



}
