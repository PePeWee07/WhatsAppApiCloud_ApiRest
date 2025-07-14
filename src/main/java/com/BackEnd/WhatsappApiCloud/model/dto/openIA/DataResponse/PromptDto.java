package com.BackEnd.WhatsappApiCloud.model.dto.openIA.DataResponse;

import java.util.Map;

public record PromptDto(
    String id,
    Map<String,Object> variables,
    String version
) {}
