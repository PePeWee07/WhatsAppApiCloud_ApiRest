package com.BackEnd.WhatsappApiCloud.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.BackEnd.WhatsappApiCloud.model.entity.tool.ToolPermissionEntity;

@Repository
public interface ToolPermissionRepository extends JpaRepository<ToolPermissionEntity, Long> {
    Optional<ToolPermissionEntity> findByToolName(String toolName);
    boolean existsByToolName(String toolName);
}
