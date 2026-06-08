package com.BackEnd.WhatsappApiCloud.model.dto.openIA.DataResponse;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PromptDto(
    String id,
    Map<String,Object> variables,
    String version
) {}
