package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.templates;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter 
@Setter 
@Builder
public class RequestTemplate {
    private String name;
    private Map<String,String> language;
    private List<TemplateComponent> components;
}