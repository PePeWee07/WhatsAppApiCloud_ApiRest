package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.AiResponseDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.AiToolCallDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageAddresDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageErrorDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessagePricingDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageRowView;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageTemplateDto;
import com.BackEnd.WhatsappApiCloud.model.entity.openIA.AiResponseEntity;
import com.BackEnd.WhatsappApiCloud.repository.AiResponseRepository;
import com.BackEnd.WhatsappApiCloud.repository.AitoolCallRepository;
import com.BackEnd.WhatsappApiCloud.repository.message.MessageAddresRespositoy;
import com.BackEnd.WhatsappApiCloud.repository.message.MessageErrorRepository;
import com.BackEnd.WhatsappApiCloud.repository.message.MessagePricingRepository;
import com.BackEnd.WhatsappApiCloud.repository.message.MessageRepository;
import com.BackEnd.WhatsappApiCloud.repository.message.MessageTemplateRepository;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.MessageHistoryService;

@Service
public class MessageHistoryServiceImpl implements MessageHistoryService {

    private final MessageRepository messageRepository;
    private final MessagePricingRepository messagePricingRepository;
    private final MessageErrorRepository messageErrorRepository;
    private final MessageAddresRespositoy messageAddresRespositoy;
    private final MessageTemplateRepository messageTemplateRepository;
    private final AiResponseRepository aiResponseRepository;
    private final AitoolCallRepository aiToolCallRepository;

    public MessageHistoryServiceImpl(
        MessageRepository messageRepository,
        MessagePricingRepository messagePricingRepository,
        MessageErrorRepository messageErrorRepository,
        MessageAddresRespositoy messageAddresRespositoy,
        MessageTemplateRepository messageTemplateRepository,
        AiResponseRepository aiResponseRepository,
        AitoolCallRepository aiToolCallRepository
    ) {
        this.messageRepository = messageRepository;
        this.messagePricingRepository = messagePricingRepository;
        this.messageErrorRepository = messageErrorRepository;
        this.messageAddresRespositoy = messageAddresRespositoy;
        this.messageTemplateRepository = messageTemplateRepository;
        this.aiResponseRepository = aiResponseRepository;
        this.aiToolCallRepository = aiToolCallRepository;
    }

    @Override
    public Page<MessageRowView> getHistoryByPhone(String phone, int page, int size, String direction) {

        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);

        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by("timestamp").ascending()
                : Sort.by("timestamp").descending(); // default desc

        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

        return messageRepository.findHistoryByPhone(phone, pageable);
    }

    @Override
    public MessagePricingDto getMessagePricingByMessageId(Long messageId) {

        return messagePricingRepository.findByMessageId(messageId)
                .map(p -> new MessagePricingDto(
                        p.getId(),
                        p.getPricingBillable(),
                        p.getPricingModel(),
                        p.getPricingCategory(),
                        p.getPricingType()))
                .orElse(null);
    }

    @Override
    public MessageErrorDto getMessageErrorByMessageId(Long messageId) {
        
        return messageErrorRepository.findByMessageId(messageId)
                .map(e -> new MessageErrorDto(
                        e.getId(),
                        e.getErrorCode(),
                        e.getErrorTitle(),
                        e.getErrorDetails(),
                        e.getErrorMessage()))
                .orElse(null);
    }

    @Override
    public MessageAddresDto getMessageAddresByMessageId(Long messageId) {
        
        return messageAddresRespositoy.findByMessageId(messageId)
                .map(a -> new MessageAddresDto(
                        a.getId(),
                        a.getLatitude(),
                        a.getLongitude(),
                        a.getLocationName(),
                        a.getLocationAddress()))
                .orElse(null);
    }

    @Override
    public MessageTemplateDto getMessageTemplateByMessageId(Long messageId) {
        
        return messageTemplateRepository.findByMessageId(messageId)
                .map(t -> new MessageTemplateDto(
                        t.getId(),
                        t.getTemplateName(),
                        t.getAnsweredAt(),
                        t.getAnswer(),
                        t.getMessageStatus()))
                .orElse(null);
    }

    
@Override
public List<AiResponseDto> getAiResponsesByMessageId(Long messageId) {

    List<AiResponseEntity> responses = aiResponseRepository.findByMessageIdOrderByCreatedAtAsc(messageId);

    return responses.stream().map(r -> {
        List<AiToolCallDto> toolDtos = aiToolCallRepository
            .findByAiResponseIdOrderByIdAsc(r.getId())
            .stream()
            .map(tc -> new AiToolCallDto(
                tc.getId(),
                tc.getCallId(),
                tc.getToolName(),
                tc.getArguments(),
                tc.getOutput()
            ))
            .collect(Collectors.toList());

        return new AiResponseDto(
            r.getId(),
            r.getResponseId(),
            r.getPreviousResponseId(),
            r.getCreatedAt() == null ? null : r.getCreatedAt().getEpochSecond(),
            r.getModel(),
            r.getPromptId(),
            r.getPromptVariables(),
            r.getPromptVersion(),
            r.getInputTokens(),
            r.getOutputTokens(),
            r.getTotalTokens(),
            r.getMetadata(),
            r.getReasoning(),
            toolDtos
        );
    }).collect(Collectors.toList());
}

}
