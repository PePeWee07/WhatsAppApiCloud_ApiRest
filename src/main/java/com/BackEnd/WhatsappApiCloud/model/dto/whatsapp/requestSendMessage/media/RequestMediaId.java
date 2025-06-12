package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.media;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class RequestMediaId implements RequestMedia {
    private String id;
    private String caption;
    // constructor de conveniencia
    public RequestMediaId(String id, String caption) {
        this.id = id;
        this.caption = caption;
    }
}
