package com.BackEnd.WhatsappApiCloud.model.dto.user;

import java.time.LocalDateTime;
import java.util.List;

import com.BackEnd.WhatsappApiCloud.model.dto.erp.ErpUserDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Este DTO reúne TODA la info que queremos devolver en /user/find:
 *  - los campos de UserChatEntity
 *  - la lista de ChatSession (como sub‐DTO)
 *  - el ErpUserDto completo (tal como viene del ERP)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserChatFullDto {
    // --- Campos de la entidad local (UserChatEntity) ---
    private Long id;
    private String whatsappPhone;
    private String threadId;
    private int limitQuestions;
    private LocalDateTime firstInteraction;
    private LocalDateTime lastInteraction;
    private LocalDateTime nextResetDate;
    private String conversationState;
    private int limitStrike;
    private boolean block;
    private String blockingReason;
    private int validQuestionCount;

    // Mapeamos chatSessions a un sub‐DTO sencillo:
    private List<ChatSessionDto> chatSessions;
    private List<UserTicketDto> userTickets;

    // ----- Datos nominales directamente con ErpUserDto -----
    private ErpUserDto erpUser;
    
}
