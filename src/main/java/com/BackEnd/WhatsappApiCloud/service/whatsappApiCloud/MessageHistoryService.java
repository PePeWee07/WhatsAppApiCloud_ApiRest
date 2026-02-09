package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud;

import java.util.List;

import org.springframework.data.domain.Page;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.AiResponseDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageAddresDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageErrorDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessagePricingDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageRowView;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageTemplateDto;

public interface MessageHistoryService {
    Page<MessageRowView> getHistoryByPhone(String phone, int page, int size, String direction);
    MessagePricingDto getMessagePricingByMessageId(Long messageId);
    MessageErrorDto getMessageErrorByMessageId(Long messageId);
    MessageAddresDto getMessageAddresByMessageId(Long messageId);
    MessageTemplateDto getMessageTemplateByMessageId(Long messageId);
    List<AiResponseDto> getAiResponsesByMessageId(Long messageId);
}
