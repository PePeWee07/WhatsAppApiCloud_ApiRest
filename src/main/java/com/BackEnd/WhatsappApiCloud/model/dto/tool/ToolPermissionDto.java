package com.BackEnd.WhatsappApiCloud.model.dto.tool;

import java.util.Set;

public record ToolPermissionDto(
        Long id,
        String toolName,
        Set<String> allowedRoles,
        boolean enabled) {
}
