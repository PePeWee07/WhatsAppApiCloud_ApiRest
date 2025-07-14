package com.BackEnd.WhatsappApiCloud.model.dto.openIA.DataResponse;

import java.util.List;
import java.util.Map;

public record DataHistoryDto(
    long created_at,
    List<Map<String,Object>> input,
    Map<String,Object> metadata,
    String model,
    List<Map<String,Object>> output,
    String previous_response_id,
    PromptDto prompt,
    ReasoningDto reasoning,
    String response_id,
    UsageDto usage
    
) {}
