package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageAddresDto {
    private Long id;
    private Double latitude;
    private Double longitude;
    private String locationName;
    private String locationAddress;
}
