package com.BackEnd.WhatsappApiCloud.model.dto.user.chatHistory;

import java.time.Instant;
import java.util.List;

public record ConversationBlockDto(
    // — campos del turno original
    int    inputTokens,
    int    outputTokens,
    int    totalTokens,
    String metadata,
    String model,
    String promptId,
    String promptVariables,
    String promptVersion,
    String responseId,
    String previousResponseId,
    Instant createdAt,
    String reasoning,

    // — agrupación
    String                       userMessage,
    List<ChatToolCallDto>        toolCalls,
    String                       assistantMessage
) {}