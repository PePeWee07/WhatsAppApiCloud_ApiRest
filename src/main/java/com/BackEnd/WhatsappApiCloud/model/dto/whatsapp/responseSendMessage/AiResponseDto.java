package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiResponseDto {
    private Long id;
    private String responseId;
    private String previousResponseId;
    private Long createdAt;
    private String model;
    private String promptId;
    private String promptVariables;
    private String promptVersion;
    private int inputTokens;
    private int outputTokens;
    private int totalTokens;
    private String metadata;
    private String reasoning;
    private List<AiToolCallDto> toolCalls;
}
