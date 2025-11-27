package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.media.*;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.templates.RequestTemplate;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class RequestMessages {
    private String messaging_product;
    private String recipient_type;
    private String to;
    private String type;  // "text", "image", "video", "sticker", "document", "template"

    private RequestContext context;

    // Media
    private RequestMessageText text;
    private RequestMedia image;
    private RequestMedia video;
    private RequestMedia document;
    private RequestSticker     sticker;       // sticker
    private RequestTemplate    template;     // template message
}
