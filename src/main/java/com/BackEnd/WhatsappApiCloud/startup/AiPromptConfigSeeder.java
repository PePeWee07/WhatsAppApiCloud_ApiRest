package com.BackEnd.WhatsappApiCloud.startup;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.BackEnd.WhatsappApiCloud.service.openAi.AiPromptConfigService;

/**
 * Al arrancar la aplicación, garantiza que exista una configuración de prompt por defecto
 * (prompt genérico) para que el primer mensaje no falle. Idempotente: si ya hay una activa,
 * no hace nada.
 */
@Component
public class AiPromptConfigSeeder implements ApplicationRunner {

    private final AiPromptConfigService service;

    public AiPromptConfigSeeder(AiPromptConfigService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        service.ensureDefault();
    }
}
