package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageErrorDto {
    private Long id;
    private String errorCode;
    private String errorTitle;
    private String errorDetails;
    private String errorMessage;
    
}
