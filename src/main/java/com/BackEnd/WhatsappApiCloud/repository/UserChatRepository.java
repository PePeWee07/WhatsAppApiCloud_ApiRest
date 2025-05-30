package com.BackEnd.WhatsappApiCloud.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatEntity;

@Repository
public interface UserChatRepository extends JpaRepository<UserChatEntity, Long> {

    /**
     * Lista paginada cargando también:
     * - chatSessions
     * - rolesUsuario
     * - rolesUsuario.detallesRol
     */
    @Override
    @EntityGraph(attributePaths = {
        "chatSessions",
        "rolesUsuario",
        "rolesUsuario.detallesRol"
    })
    Page<UserChatEntity> findAll(Pageable pageable);

    /**
     * Búsqueda por número de cédula (identificacion)
     */
    @EntityGraph(attributePaths = {
        "chatSessions",
        "rolesUsuario",
        "rolesUsuario.detallesRol"
    })
    Optional<UserChatEntity> findByIdentificacion(String identificacion);

    /**
     * Búsqueda por teléfono de WhatsApp
     */
    @EntityGraph(attributePaths = {
        "chatSessions",
        "rolesUsuario",
        "rolesUsuario.detallesRol"
    })
    Optional<UserChatEntity> findByWhatsappPhone(String whatsappPhone);

    @EntityGraph(attributePaths = {
        "rolesUsuario",
        "rolesUsuario.detallesRol"
    })
    Optional<UserChatEntity> findWithRolesByWhatsappPhone(String whatsappPhone);
}
