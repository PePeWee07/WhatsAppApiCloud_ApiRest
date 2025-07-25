package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.templates;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter @Setter @Builder
public class TemplateComponent {
    private String type;           // "header", "body", "footer", "button"
    private String sub_type;       // sólo para botón ("FLOW")
    private String index;          // sólo para botón ("0")
    private List<ComponentParameter> parameters;
}