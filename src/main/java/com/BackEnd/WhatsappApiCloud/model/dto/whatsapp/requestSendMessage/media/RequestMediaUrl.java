package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.media;


import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class RequestMediaUrl implements RequestMedia {
    private String link;
    private String caption;

    public RequestMediaUrl(String link, String caption) {
        this.link = link;
        this.caption = caption;
    }
}
