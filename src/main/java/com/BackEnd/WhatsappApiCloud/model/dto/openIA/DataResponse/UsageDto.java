package com.BackEnd.WhatsappApiCloud.model.dto.openIA.DataResponse;

public record UsageDto(
    int input_tokens,
    int output_tokens,
    int total_tokens
) {}