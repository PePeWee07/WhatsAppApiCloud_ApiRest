package com.BackEnd.WhatsappApiCloud.model.entity.tool;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Última ejecución de una tool por usuario. Sirve para calcular el enfriamiento (cooldown)
 * entre interacciones. La config del cooldown (segundos) vive en ToolPermissionEntity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tool_usage",
       uniqueConstraints = @UniqueConstraint(columnNames = {"whatsapp_phone", "tool_name"}))
public class ToolUsageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "whatsapp_phone", nullable = false)
    private String whatsappPhone;

    @Column(name = "tool_name", nullable = false)
    private String toolName;

    @Column(name = "last_executed_at")
    private Instant lastExecutedAt;
}
