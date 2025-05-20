package com.BackEnd.WhatsappApiCloud.service.userChat.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatEntity;
import com.BackEnd.WhatsappApiCloud.repository.UserChatRepository;
import com.BackEnd.WhatsappApiCloud.service.userChat.UserchatService;

@Service
public class UserChatImpl implements UserchatService {

    private final UserChatRepository repo;

    public UserChatImpl(UserChatRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserChatEntity findByCedula(String cedula) {
        UserChatEntity resp = repo.findByCedula(cedula)
                .orElseThrow(() -> new RuntimeException("User not found with cedula: " + cedula));
        return resp;
    }

    @Override
    public UserChatEntity findByPhone(String phone) {
        UserChatEntity resp = repo.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found with phone: " + phone));
        return resp;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserChatEntity> findAll(int page, int size,
                                        String sortBy, String direction) {
        Sort sort = Sort.by(sortBy);
        sort = "desc".equalsIgnoreCase(direction)
            ? sort.descending()
            : sort.ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserChatEntity> p = repo.findAll(pageable);

        p.getContent().forEach(u -> u.getChatSessions().size());

        return p;
    }

}
