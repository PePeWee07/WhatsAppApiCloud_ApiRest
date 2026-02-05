package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageRowView;
import com.BackEnd.WhatsappApiCloud.repository.MessageRepository;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.MessageHistoryService;

@Service
public class MessageHistoryServiceImpl implements MessageHistoryService {

    private final MessageRepository messageRepository;

    public MessageHistoryServiceImpl(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
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

}
