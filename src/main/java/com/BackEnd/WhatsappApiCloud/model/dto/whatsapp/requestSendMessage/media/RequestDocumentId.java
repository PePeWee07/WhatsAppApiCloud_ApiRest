package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.media;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class RequestDocumentId implements RequestMedia {
    private String id;
    private String caption;
    private String filename;

    public RequestDocumentId(String id, String caption, String filename) {
        this.id = id;
        this.caption = caption;
        this.filename = filename;
    }
}
