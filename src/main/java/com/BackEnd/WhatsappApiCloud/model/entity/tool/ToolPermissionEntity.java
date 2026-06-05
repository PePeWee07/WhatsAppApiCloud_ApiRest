package com.BackEnd.WhatsappApiCloud.model.entity.tool;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Permisos por herramienta (tool) de Gpt-Tics, gestionados dinámicamente desde el core.
 * - toolName: nombre exacto de la tool registrada en Gpt-Tics (ej. "send_support_email").
 * - allowedRoles: roles (en MAYÚSCULAS) que pueden invocarla.
 * - enabled: si está en false, la tool queda deshabilitada (se deniega a todos).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tool_permissions")
public class ToolPermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tool_name", unique = true, nullable = false)
    private String toolName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "tool_permission_roles",
        joinColumns = @JoinColumn(name = "tool_permission_id")
    )
    @Column(name = "role")
    private Set<String> allowedRoles = new HashSet<>();

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    // Segundos de enfriamiento entre ejecuciones de esta tool (0 = sin cooldown / concurrente).
    @Column(name = "cooldown_seconds")
    private Integer cooldownSeconds = 0;
}
