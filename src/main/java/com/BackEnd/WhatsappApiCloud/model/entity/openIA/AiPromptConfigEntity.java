package com.BackEnd.WhatsappApiCloud.model.entity.openIA;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuración del prompt/asistente CatIA gestionada dinámicamente desde el core.
 * Reemplaza al "prompt object" de OpenAI. Editable en caliente vía REST y cacheada en Redis.
 *
 * Las instrucciones admiten placeholders que el core interpola con los datos del usuario:
 * {{names}}, {{phone}}, {{roles}}, {{identificacion}}, {{email_institucional}},
 * {{email_personal}}, {{sexo}}.
 *
 * reasoning / tools / include / text se almacenan como JSON (texto) y se reenvían tal cual a OpenAI.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_prompt_config")
public class AiPromptConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "version", nullable = false)
    private int version = 1;

    @Column(name = "instructions", columnDefinition = "TEXT", nullable = false)
    private String instructions;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "top_p")
    private Double topP;

    @Column(name = "max_output_tokens")
    private Integer maxOutputTokens;

    /** JSON con la config de razonamiento, p. ej. {"effort":"medium","summary":"auto"}. */
    @Column(name = "reasoning_json", columnDefinition = "TEXT")
    private String reasoningJson;

    /** JSON array con la definición de tools enviada a OpenAI. */
    @Column(name = "tools_json", columnDefinition = "TEXT")
    private String toolsJson;

    /** JSON array con las opciones 'include'. */
    @Column(name = "include_json", columnDefinition = "TEXT")
    private String includeJson;

    /** JSON con el parámetro 'text', p. ej. {"format":{"type":"text"},"verbosity":"medium"}. */
    @Column(name = "text_json", columnDefinition = "TEXT")
    private String textJson;

    @Column(name = "store", nullable = false)
    private boolean store = true;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
