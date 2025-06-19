package com.BackEnd.WhatsappApiCloud.model.dto.glpi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolutionDecisionRequest {
    private Boolean accepted;
    private Long ticketId;
    private String content;
}
