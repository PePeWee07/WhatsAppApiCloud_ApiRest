package com.BackEnd.WhatsappApiCloud.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.BackEnd.WhatsappApiCloud.model.entity.User.UserChatEntity;

@Repository
public interface UserChatRepository extends JpaRepository<UserChatEntity, Long> {
    Optional<UserChatEntity> findByCedula(String cedula);
    Optional<UserChatEntity> findByPhone(String phone);
}
