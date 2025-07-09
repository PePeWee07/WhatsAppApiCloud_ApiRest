package com.BackEnd.WhatsappApiCloud.model.dto.user;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public  class UserChatSessionDto {
    private Long id;
    private String whatsappPhone;
    private int messageCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
