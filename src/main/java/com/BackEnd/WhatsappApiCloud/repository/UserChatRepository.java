package com.BackEnd.WhatsappApiCloud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.BackEnd.WhatsappApiCloud.model.entity.UserChatEntity;

@Repository
public interface UserChatRepository extends JpaRepository<UserChatEntity, Long> {
    
}
