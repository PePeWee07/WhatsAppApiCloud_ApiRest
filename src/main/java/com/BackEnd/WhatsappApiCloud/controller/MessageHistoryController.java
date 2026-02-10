package com.BackEnd.WhatsappApiCloud.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.AiResponseDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageAddresDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageErrorDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessagePricingDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageRowView;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.MessageHistoryService;

@RequestMapping("/api/v1/messages")
@RestController
public class MessageHistoryController {

    private final MessageHistoryService historyService;

    public MessageHistoryController(MessageHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/history")
    public Page<MessageRowView> getHistory(
        @RequestParam String phone,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(defaultValue = "desc") String direction) {
        return historyService.getHistoryByPhone(phone, page, size, direction);
    }

    @GetMapping("/{id}/pricing")
    public MessagePricingDto getPricing(@PathVariable("id") Long messageId) {
        if (messageId == null || messageId <= 0) {
            throw new IllegalArgumentException("Invalid message ID");
        }
        return historyService.getMessagePricingByMessageId(messageId);
    }

    @GetMapping("/{id}/error")
    public MessageErrorDto getError(@PathVariable("id") Long messageId) {
        if (messageId == null || messageId <= 0) {
            throw new IllegalArgumentException("Invalid message ID");
        }
        return historyService.getMessageErrorByMessageId(messageId);
    }

    @GetMapping("/{id}/address")
    public MessageAddresDto getAddres(@PathVariable("id") Long messageId)
    {
        if (messageId == null || messageId <= 0) {
            throw new IllegalArgumentException("Invalid message ID");
        }

        return historyService.getMessageAddresByMessageId(messageId);
    }

    @GetMapping("/{id}/ai-response")
    public List<AiResponseDto> getAiResponses(@PathVariable("id") Long messageId) {
        return historyService.getAiResponsesByMessageId(messageId);
    }

    @GetMapping("/{id}")
    public MessageDto getMessageDetails(@PathVariable("id") Long messageId) {
        return historyService.getMessageDetailsById(messageId);
    }

}
