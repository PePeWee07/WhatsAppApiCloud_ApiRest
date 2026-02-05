package com.BackEnd.WhatsappApiCloud.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.BackEnd.WhatsappApiCloud.service.sse.MessageEventStreamService;

@RequestMapping("/api/v1")
@RestController
public class MessageEventStreamController {
    private final MessageEventStreamService sseService;

    public MessageEventStreamController(MessageEventStreamService sseService) {
        this.sseService = sseService;
    }

    @GetMapping("/messages/stream")
    public SseEmitter streamMessages(@RequestParam("phone") String phone) {
        return sseService.subscribe(phone);
    }
}
