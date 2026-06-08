package com.BackEnd.WhatsappApiCloud.model.dto.openIA.DataResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UsageDto(
    int input_tokens,
    int output_tokens,
    int total_tokens
) {}