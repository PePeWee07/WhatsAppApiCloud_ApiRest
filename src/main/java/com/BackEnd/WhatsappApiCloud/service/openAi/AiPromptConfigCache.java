package com.BackEnd.WhatsappApiCloud.service.openAi;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.BackEnd.WhatsappApiCloud.exception.BadRequestException;
import com.BackEnd.WhatsappApiCloud.model.dto.openIA.promptConfig.PromptConfigSnapshot;
import com.BackEnd.WhatsappApiCloud.model.entity.openIA.AiPromptConfigEntity;
import com.BackEnd.WhatsappApiCloud.repository.AiPromptConfigRepository;

/**
 * Bean dedicado a la lectura CACHEADA de la config activa.
 *
 * Va en su propio bean a propósito: si getActiveSnapshot() se invocara desde otro método de la
 * misma clase (self-invocation), Spring NO aplicaría el proxy de caché. Al estar aquí,
 * AiPromptConfigService lo llama como dependencia (cross-bean) y el @Cacheable sí funciona.
 *
 * La caché (AiPromptConfigService.CACHE_NAME) se evicta en cada edición desde el servicio.
 */
@Service
public class AiPromptConfigCache {

    private final AiPromptConfigRepository repo;

    public AiPromptConfigCache(AiPromptConfigRepository repo) {
        this.repo = repo;
    }

    @Cacheable(value = AiPromptConfigService.CACHE_NAME, key = "'prompt'")
    @Transactional(readOnly = true)
    public PromptConfigSnapshot getActiveSnapshot() {
        AiPromptConfigEntity e = repo.findByActiveTrue()
                .orElseThrow(() -> new BadRequestException(
                        "No hay una configuración de prompt activa. Ejecuta el seeder o POST /api/v1/ai-prompt-config/seed-default."));
        return new PromptConfigSnapshot(
                e.getInstructions(),
                e.getModel(),
                e.getTemperature(),
                e.getTopP(),
                e.getMaxOutputTokens(),
                e.getReasoningJson(),
                e.getToolsJson(),
                e.getIncludeJson(),
                e.isStore(),
                e.getTextJson());
    }
}
