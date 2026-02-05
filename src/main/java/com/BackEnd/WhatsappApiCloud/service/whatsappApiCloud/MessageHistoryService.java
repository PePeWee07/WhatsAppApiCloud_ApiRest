package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud;

import org.springframework.data.domain.Page;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageRowView;

public interface MessageHistoryService {
    Page<MessageRowView> getHistoryByPhone(String phone, int page, int size, String direction);
}
