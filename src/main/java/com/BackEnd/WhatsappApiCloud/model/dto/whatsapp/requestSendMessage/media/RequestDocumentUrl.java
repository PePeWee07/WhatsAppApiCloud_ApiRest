package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.media;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class RequestDocumentUrl implements RequestMedia {
    private String link;
    private String caption;
    private String filename;

    public RequestDocumentUrl(String link, String caption, String filename) {
        this.link = link;
        this.caption = caption;
        this.filename = filename;
    }
}
