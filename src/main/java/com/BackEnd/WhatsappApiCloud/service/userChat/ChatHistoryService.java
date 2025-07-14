package com.BackEnd.WhatsappApiCloud.service.userChat;

import java.util.List;

import com.BackEnd.WhatsappApiCloud.model.dto.openIA.AnswersOpenIADto;
import com.BackEnd.WhatsappApiCloud.model.dto.user.chatHistory.ConversationBlockDto;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface ChatHistoryService {
    void saveHistory(AnswersOpenIADto payload, String whatsappPhone) throws JsonProcessingException;
    List<ConversationBlockDto> getConversationBlocks(String whatsappPhone);
}