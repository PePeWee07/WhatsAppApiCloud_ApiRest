package com.BackEnd.WhatsappApiCloud.model.dto.user.chatHistory;

public record ChatToolCallDto(
    String callId,
    String toolName,
    String arguments,
    String output
) {}
