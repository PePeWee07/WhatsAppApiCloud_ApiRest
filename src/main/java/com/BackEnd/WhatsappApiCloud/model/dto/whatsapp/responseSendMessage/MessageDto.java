package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage;

import com.BackEnd.WhatsappApiCloud.util.enums.MessageDirectionEnum;
import com.BackEnd.WhatsappApiCloud.util.enums.MessageSourceEnum;
import com.BackEnd.WhatsappApiCloud.util.enums.MessageTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {

    private Long id;
    private String wamid;

    private String profileName;
    private String conversationUserPhone;
    private String fromPhone;
    private String toPhone;
    private MessageDirectionEnum direction;
    private MessageSourceEnum source;

    private Long timestamp;
    private Long sentAt;
    private Long deliveredAt;
    private Long readAt;
    private Long failedAt;

    private String mediaId;
    private String mediaMimeType;
    private String mediaFilename;
    private String mediaCaption;
    
    private MessageTypeEnum type;
    private String relatedWamid;
    private String reactionEmoji;
    private String textBody;
    
    private int aiResponseCount;
    private MessageTemplateDto messageTemplate;
    private MessagePricingDto messagePricing;
    private MessageAddresDto messageAddres;
    private MessageErrorDto messageError;

}
