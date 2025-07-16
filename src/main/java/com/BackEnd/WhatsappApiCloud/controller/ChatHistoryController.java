package com.BackEnd.WhatsappApiCloud.controller;

import com.BackEnd.WhatsappApiCloud.model.dto.user.chatHistory.ConversationBlockDto;
import com.BackEnd.WhatsappApiCloud.service.userChat.ChatHistoryService;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ChatHistoryController {

    private final ChatHistoryService historyService;
    private static final int MAX_PAGE_SIZE = 100;

    public ChatHistoryController(ChatHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/history/{phone}")
    public Page<ConversationBlockDto> getByPhone(
        @PathVariable String phone,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        return historyService.getConversationBlocks(phone, page, safeSize);
    }

}