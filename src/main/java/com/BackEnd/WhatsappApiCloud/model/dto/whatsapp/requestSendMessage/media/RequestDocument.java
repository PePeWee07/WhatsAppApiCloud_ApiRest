package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.media;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RequestDocument {
    private String id;
    private String caption;
    private String filename;
}