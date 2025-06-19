package com.BackEnd.WhatsappApiCloud.model.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTicketDto {
    private Long id;

    private String name;

    private String status;
}
