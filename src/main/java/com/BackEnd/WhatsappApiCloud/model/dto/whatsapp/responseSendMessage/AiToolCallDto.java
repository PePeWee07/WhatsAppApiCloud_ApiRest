package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiToolCallDto {
    private Long id;
    private String callId;
    private String toolName;
    private String arguments;
    private String output;
}
