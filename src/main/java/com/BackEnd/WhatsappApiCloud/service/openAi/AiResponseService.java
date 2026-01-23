package com.BackEnd.WhatsappApiCloud.service.openAi;
import com.BackEnd.WhatsappApiCloud.model.dto.openIA.AnswersOpenIADto;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageEntity;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface AiResponseService {
    void saveAiResponses(AnswersOpenIADto payload, MessageEntity message) throws JsonProcessingException;
}