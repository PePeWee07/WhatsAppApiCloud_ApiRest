package com.BackEnd.WhatsappApiCloud.controller;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageRowView;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.MessageHistoryService;

@RequestMapping("/api/v1")
@RestController
public class MessageHistoryController {

    private final MessageHistoryService historyService;

    public MessageHistoryController(MessageHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/messages/history")
    public Page<MessageRowView> getHistory(
        @RequestParam String phone,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(defaultValue = "desc") String direction) {
        return historyService.getHistoryByPhone(phone, page, size, direction);
    }
}
