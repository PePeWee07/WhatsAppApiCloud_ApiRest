package com.BackEnd.WhatsappApiCloud.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.BackEnd.WhatsappApiCloud.model.dto.openIA.promptConfig.AiPromptConfigDto;
import com.BackEnd.WhatsappApiCloud.model.dto.openIA.promptConfig.AiPromptConfigRequest;
import com.BackEnd.WhatsappApiCloud.service.openAi.AiPromptConfigService;

/**
 * Administración de la configuración del prompt de CatIA (instrucciones, modelo, parámetros y tools).
 * Editar aquí surte efecto en el siguiente mensaje SIN redesplegar (se evicta la caché al guardar).
 */
@RestController
@RequestMapping("/api/v1/ai-prompt-config")
public class AiPromptConfigController {

    private final AiPromptConfigService service;

    public AiPromptConfigController(AiPromptConfigService service) {
        this.service = service;
    }

    /** Config activa actual (la que se envía a Gpt-Tics en cada mensaje). */
    @GetMapping
    public ResponseEntity<AiPromptConfigDto> getActive() {
        return ResponseEntity.ok(service.getActive());
    }

    /** Edita la config activa (parcial: solo los campos no nulos). Sube versión y evicta la caché. */
    @PutMapping
    public ResponseEntity<AiPromptConfigDto> update(@RequestBody AiPromptConfigRequest body) {
        return ResponseEntity.ok(service.updateActive(body));
    }

    /** Crea la config por defecto (prompt genérico) si aún no existe. Idempotente. */
    @PostMapping("/seed-default")
    public ResponseEntity<AiPromptConfigDto> seedDefault() {
        service.ensureDefault();
        return ResponseEntity.ok(service.getActive());
    }
}
