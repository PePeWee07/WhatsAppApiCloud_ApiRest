package com.BackEnd.WhatsappApiCloud.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.BackEnd.WhatsappApiCloud.model.dto.tool.ToolPermissionDto;
import com.BackEnd.WhatsappApiCloud.service.tools.ToolPermissionService;

@RestController
@RequestMapping("/api/v1/tool-permissions")
public class ToolPermissionController {

    private final ToolPermissionService service;

    public ToolPermissionController(ToolPermissionService service) {
        this.service = service;
    }

    // Listar todas las tools con sus roles y estado
    @GetMapping
    public ResponseEntity<List<ToolPermissionDto>> list() {
        return ResponseEntity.ok(service.listAll());
    }

    // Ver el mapa tal cual se envía a Gpt-Tics (útil para depurar)
    @GetMapping("/map")
    public ResponseEntity<Map<String, List<String>>> map() {
        return ResponseEntity.ok(service.getPermissionsMap());
    }

    // Crear o actualizar roles (y opcionalmente enabled) de una tool
    @PutMapping("/{toolName}")
    public ResponseEntity<ToolPermissionDto> upsert(
            @PathVariable String toolName,
            @RequestBody ToolPermissionUpsertRequest body) {
        return ResponseEntity.ok(service.upsert(toolName, body.allowedRoles(), body.enabled()));
    }

    // Habilitar / deshabilitar una tool
    @PatchMapping("/{toolName}/enabled")
    public ResponseEntity<ToolPermissionDto> setEnabled(
            @PathVariable String toolName,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(service.setEnabled(toolName, enabled));
    }

    @DeleteMapping("/{toolName}")
    public ResponseEntity<Void> delete(@PathVariable String toolName) {
        service.delete(toolName);
        return ResponseEntity.noContent().build();
    }

    // Restaurar de fábrica: sobrescribe las tools básicas a sus roles por defecto (enabled=true)
    @PostMapping("/restore-defaults")
    public ResponseEntity<List<ToolPermissionDto>> restoreDefaults() {
        return ResponseEntity.ok(service.restoreDefaults());
    }

    public record ToolPermissionUpsertRequest(Set<String> allowedRoles, Boolean enabled) {}
}
