package com.BackEnd.WhatsappApiCloud.model.dto.openIA;

import java.util.List;

import com.BackEnd.WhatsappApiCloud.model.dto.openIA.DataResponse.DataHistoryDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnswersOpenIADto(
        String answer,
        String previousResponseId,
        List<DataHistoryDto> data,
        List<String> executedTools) {

}
