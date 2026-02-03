package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage;


import java.time.Instant;

import com.BackEnd.WhatsappApiCloud.util.enums.MessageDirectionEnum;
import com.BackEnd.WhatsappApiCloud.util.enums.MessageSourceEnum;
import com.BackEnd.WhatsappApiCloud.util.enums.MessageTypeEnum;

public interface MessageRowView {

    Long getId();
    String getWamid();

    String getProfileName();
    String getConversationUserPhone();
    String getFromPhone();
    String getToPhone();
    MessageDirectionEnum getDirection();
    MessageSourceEnum getSource();
    MessageTypeEnum getType();

    Instant getTimestamp();
    Instant getSentAt();
    Instant getDeliveredAt();
    Instant getReadAt();
    Instant getFailedAt();

    String getMediaId();
    String getMediaMimeType();
    String getMediaFilename();
    String getMediaCaption();

    String getRelatedWamid();
    String getReactionEmoji();
    String getTextBody();

    Integer getAiResponseCount();
    Boolean getHasTemplate();
    Boolean getHasPricing();
    Boolean getHasAddres();
    Boolean getHasError();
}
