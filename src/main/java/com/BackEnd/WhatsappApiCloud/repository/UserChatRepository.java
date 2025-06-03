package com.BackEnd.WhatsappApiCloud.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatEntity;

@Repository
public interface UserChatRepository extends JpaRepository<UserChatEntity, Long> {
    
    // @EntityGraph(attributePaths = "chatSessions")
    Page<UserChatEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "chatSessions")
    Optional<UserChatEntity> findByCedula(String cedula);

    @EntityGraph(attributePaths = "chatSessions")
    Optional<UserChatEntity> findByPhone(String phone);

         Page<UserChatEntity> findByThreadIdIsNotNullAndLastInteractionBetween(
        LocalDateTime inicio,
        LocalDateTime fin,
        Pageable pageable);
}
