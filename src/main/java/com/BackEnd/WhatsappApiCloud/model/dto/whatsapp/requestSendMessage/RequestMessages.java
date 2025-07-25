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

    private RequestMessageText text;    // Usado cuando type = "text"
    private RequestMedia image;         // Usado cuando type = "document"
    private RequestVideoLink video;     // Usado cuando type = "video"
    private RequestMediaLink sticker;   // Usado cuando type = "sticker"
    private RequestDocument document;   // Usado cuando type = "document"
    private RequestTemplate template;   // Usado cuando type = "template"
}
