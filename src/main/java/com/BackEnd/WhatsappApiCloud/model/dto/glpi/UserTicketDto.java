package com.BackEnd.WhatsappApiCloud.model.dto.glpi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTicketDto {
    private Long id;

    private String whatsappPhone;

    private String name;

    private String status;

    private String date_creation;

    private String closedate;

    private String solvedate;

    private String date_mod;
}
