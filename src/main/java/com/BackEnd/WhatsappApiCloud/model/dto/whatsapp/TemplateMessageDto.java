package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateMessageDto {

    private Long id;
    private String toPhone;
    private String templateName;
    private LocalDateTime sentAt;
    private LocalDateTime answeredAt;
    private String wamid;
    private String answer;
    private String messageStatus;
    
}
