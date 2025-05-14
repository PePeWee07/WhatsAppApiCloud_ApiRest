package com.BackEnd.WhatsappApiCloud.service.userChat.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.BackEnd.WhatsappApiCloud.model.entity.User.UserChatEntity;
import com.BackEnd.WhatsappApiCloud.repository.UserChatRepository;
import com.BackEnd.WhatsappApiCloud.service.userChat.UserchatService;

@Service
public class UserChatImpl implements UserchatService {

    @Autowired
    private UserChatRepository userChatRepository;

    @Override
    public UserChatEntity findByCedula(String cedula) {
        UserChatEntity resp = userChatRepository.findByCedula(cedula)
                .orElseThrow(() -> new RuntimeException("User not found with cedula: " + cedula));
        return resp;
    }

    @Override
    public UserChatEntity findByPhone(String phone) {
        UserChatEntity resp = userChatRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found with phone: " + phone));
        return resp;
    }

    
    
}
