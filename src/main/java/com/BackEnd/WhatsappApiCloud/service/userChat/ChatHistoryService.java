package com.BackEnd.WhatsappApiCloud.service.userChat;

import org.springframework.data.domain.Page;

import com.BackEnd.WhatsappApiCloud.model.dto.openIA.AnswersOpenIADto;
import com.BackEnd.WhatsappApiCloud.model.dto.user.chatHistory.ConversationBlockDto;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface ChatHistoryService {
    void saveHistory(AnswersOpenIADto payload, String whatsappPhone) throws JsonProcessingException;
    Page<ConversationBlockDto> getConversationBlocks(String whatsappPhone, int page, int size);
}