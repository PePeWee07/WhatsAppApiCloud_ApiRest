package com.BackEnd.WhatsappApiCloud.model.dto.openIA;

import java.util.List;

import com.BackEnd.WhatsappApiCloud.model.dto.openIA.DataResponse.DataHistoryDto;

public record AnswersOpenIADto(String answer, String previousResponseId, List<DataHistoryDto> data) {
    
}
