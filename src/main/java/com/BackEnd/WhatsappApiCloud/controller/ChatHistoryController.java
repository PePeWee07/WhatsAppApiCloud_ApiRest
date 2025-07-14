package com.BackEnd.WhatsappApiCloud.controller;

import com.BackEnd.WhatsappApiCloud.model.dto.user.chatHistory.ConversationBlockDto;
import com.BackEnd.WhatsappApiCloud.service.userChat.ChatHistoryService;

import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatHistoryController {

    private final ChatHistoryService historyService;

    public ChatHistoryController(ChatHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/history/{phone}")
    public List<ConversationBlockDto> getByPhone(@PathVariable String phone) {
        return historyService.getConversationBlocks(phone);
    }

    

}