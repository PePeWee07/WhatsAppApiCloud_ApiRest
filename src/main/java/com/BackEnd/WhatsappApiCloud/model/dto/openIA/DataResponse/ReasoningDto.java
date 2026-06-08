package com.BackEnd.WhatsappApiCloud.model.dto.openIA.DataResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReasoningDto(
    Object effort,
    Object summary
) {}
