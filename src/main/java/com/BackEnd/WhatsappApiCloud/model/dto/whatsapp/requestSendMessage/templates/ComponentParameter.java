package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.templates;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter @Setter @Builder
public class ComponentParameter {
    private String type;           // "text", "image", "payload"
    private String text;           // para parámetros de texto
    private String payload;        // para botones
    private Map<String,String> image; // para headers de imagen { "id o link": mediaId }
}
