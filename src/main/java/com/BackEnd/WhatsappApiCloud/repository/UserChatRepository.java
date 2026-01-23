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

    @Override
    Page<UserChatEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "chatSessions")
    Optional<UserChatEntity> findByIdentificacion(String identificacion);

    @EntityGraph(attributePaths = "chatSessions")
    Optional<UserChatEntity> findByWhatsappPhone(String whatsappPhone);

    Page<UserChatEntity> findByPreviousResponseIdIsNotNullAndLastInteractionBetween(
            LocalDateTime inicio,
            LocalDateTime fin,
            Pageable pageable);

    @Query("""
                        SELECT DISTINCT u
                        FROM UserChatEntity u
                        JOIN u.chatSessions cs
                        WHERE cs.startTime >= :inicio
                          AND cs.startTime <  :fin
                    """)
    Page<UserChatEntity> findByChatSessionsStartInRange(
                    @Param("inicio") LocalDateTime inicio,
                    @Param("fin") LocalDateTime fin,
                    Pageable pageable);

}
