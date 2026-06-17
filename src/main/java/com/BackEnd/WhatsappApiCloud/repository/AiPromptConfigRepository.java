package com.BackEnd.WhatsappApiCloud.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.BackEnd.WhatsappApiCloud.model.entity.openIA.AiPromptConfigEntity;

@Repository
public interface AiPromptConfigRepository extends JpaRepository<AiPromptConfigEntity, Long> {

    Optional<AiPromptConfigEntity> findByActiveTrue();

    Optional<AiPromptConfigEntity> findByName(String name);
}
