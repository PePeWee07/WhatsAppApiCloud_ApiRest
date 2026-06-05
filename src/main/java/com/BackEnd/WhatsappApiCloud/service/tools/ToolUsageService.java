package com.BackEnd.WhatsappApiCloud.service.tools;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.BackEnd.WhatsappApiCloud.model.entity.tool.ToolUsageEntity;
import com.BackEnd.WhatsappApiCloud.repository.ToolUsageRepository;

@Service
public class ToolUsageService {

    private final ToolUsageRepository repo;

    public ToolUsageService(ToolUsageRepository repo) {
        this.repo = repo;
    }

    /**
     * Segundos de enfriamiento que le quedan a cada tool para este usuario.
     * Solo incluye las que SIGUEN en cooldown (remaining > 0); las demás se omiten (= 0).
     *
     * @param cooldownConfig mapa toolName -> segundos de cooldown configurados.
     */
    @Transactional(readOnly = true)
    public Map<String, Integer> getRemainingMap(String phone, Map<String, Integer> cooldownConfig) {
        Map<String, Integer> remaining = new HashMap<>();
        if (cooldownConfig == null || cooldownConfig.isEmpty()) {
            return remaining;
        }
        Instant now = Instant.now();
        for (ToolUsageEntity u : repo.findByWhatsappPhone(phone)) {
            int cd = cooldownConfig.getOrDefault(u.getToolName(), 0);
            if (cd <= 0 || u.getLastExecutedAt() == null) {
                continue;
            }
            long elapsed = Duration.between(u.getLastExecutedAt(), now).getSeconds();
            long rem = cd - elapsed;
            if (rem > 0) {
                remaining.put(u.getToolName(), (int) rem);
            }
        }
        return remaining;
    }

    /** Marca last_executed_at = ahora para las tools ejecutadas (upsert por usuario+tool). */
    @Transactional
    public void recordExecutions(String phone, List<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        for (String tool : toolNames) {
            if (tool == null || tool.isBlank()) {
                continue;
            }
            ToolUsageEntity u = repo.findByWhatsappPhoneAndToolName(phone, tool)
                    .orElseGet(() -> {
                        ToolUsageEntity n = new ToolUsageEntity();
                        n.setWhatsappPhone(phone);
                        n.setToolName(tool);
                        return n;
                    });
            u.setLastExecutedAt(now);
            repo.save(u);
        }
    }
}
