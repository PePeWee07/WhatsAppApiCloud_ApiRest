package com.BackEnd.WhatsappApiCloud.service.userChat.impl;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatEntity;
import com.BackEnd.WhatsappApiCloud.repository.UserChatRepository;
import com.BackEnd.WhatsappApiCloud.service.userChat.UserchatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserChatImpl implements UserchatService {

    private final UserChatRepository repo;
    private final ObjectMapper objectMapper;

    public UserChatImpl(UserChatRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    @Override
    public UserChatEntity findByCedula(String cedula) {
        UserChatEntity resp = repo.findByIdentificacion(cedula)
                .orElseThrow(() -> new RuntimeException("User not found with cedula: " + cedula));
        return resp;
    }

    @Override
    public UserChatEntity findByPhone(String phone) {
        UserChatEntity resp = repo.findByWhatsappPhone(phone)
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

    @Override
    @Transactional
    public UserChatEntity patchUser(Long id, Map<String, Object> updates) {
        UserChatEntity user = repo.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado con id " + id));

        try {
            objectMapper.readerForUpdating(user).readValue(objectMapper.writeValueAsString(updates));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error aplicando patch al usuario", e);
        }

        UserChatEntity saved = repo.save(user);
        saved.getChatSessions().size();

        return saved;
    }

}
