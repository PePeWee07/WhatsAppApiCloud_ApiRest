package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.media;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class RequestSticker implements RequestMedia {
    private String link;

    public RequestSticker(String link) {
        this.link = link;
    }
}
