package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateMessageDto {
    private Long id;                    // id de messages_templates
    private String toPhone;             // MessageEntity.toPhone
    private String templateName;        // MessageTemplateEntity.templateName
    private LocalDateTime sentAt;       // MessageEntity (sentAt/timestamp)
    private LocalDateTime answeredAt;   // MessageTemplateEntity.answeredAt
    private String wamid;               // MessageEntity.messageId
    private String answer;              // MessageTemplateEntity.answer
    private String messageStatus;       // MessageTemplateEntity.messageStatus
}
