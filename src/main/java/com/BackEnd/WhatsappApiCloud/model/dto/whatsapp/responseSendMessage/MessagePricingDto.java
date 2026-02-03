package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessagePricingDto {
    private Long id;
    private Boolean pricingBillable;
    private String pricingModel;
    private String pricingCategory;
    private String pricingType;
    
}
