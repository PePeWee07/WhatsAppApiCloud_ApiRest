package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage;

import java.util.List;
import java.util.Map;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.media.*;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.templates.ComponentParameter;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.templates.RequestTemplate;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.templates.TemplateComponent;

public class RequestMessagesFactory {

    public static RequestMessages buildImageByIdWithText(String to, String mediaId, String caption) {
        RequestMessages req = base(to, "image");
        req.setImage(new RequestMediaId(mediaId, caption));
        return req;
    }

    public static RequestMessages buildDocumentByIdWithText(String to, String documentId, String caption, String filename) {
        RequestMessages req = base(to, "document");
        req.setDocument(new RequestDocument(documentId, caption, filename));
        return req;
    }

    public static RequestMessages buildTextMessage(String to, String message) {
        RequestMessages req = base(to, "text");
        req.setText(new RequestMessageText(false, message));
        return req;
    }

    public static RequestMessages buildImageByUrl(String to, String url) {
        RequestMessages req = base(to, "image");
        req.setImage(new RequestMediaLink(url));
        return req;
    }

    public static RequestMessages buildVideoByUrl(String to, String url, String caption) {
        RequestMessages req = base(to, "video");
        req.setVideo(new RequestVideoLink(url, caption));
        return req;
    }

    public static RequestMessages buildStickerByUrl(String to, String url) {
        RequestMessages req = base(to, "sticker");
        req.setSticker(new RequestMediaLink(url));
        return req;
    }

    /**
     * Construye un mensaje de tipo plantilla (template).
     */
    public static RequestMessages buildTemplateMessage(
            String to,
            String templateName,
            String languageCode,
            String headerMediaId,
            String bodyText,
            String footerText,
            String buttonPayload
    ) {
        RequestMessages msg = base(to, "template");

        // Header
        TemplateComponent header = TemplateComponent.builder()
            .type("header")
            .parameters(List.of(
                ComponentParameter.builder()
                    .type("image")
                    .image(Map.of("id", headerMediaId))
                    .build()
            ))
            .build();

        // Body
        TemplateComponent body = TemplateComponent.builder()
            .type("body")
            .parameters(List.of(
                ComponentParameter.builder()
                    .type("text")
                    .text(bodyText)
                    .build()
            ))
            .build();

        // Footer
        TemplateComponent footer = TemplateComponent.builder()
            .type("footer")
            .parameters(List.of(
                ComponentParameter.builder()
                    .type("text")
                    .text(footerText)
                    .build()
            ))
            .build();

        // Button
        TemplateComponent button = TemplateComponent.builder()
            .type("button")
            .sub_type("FLOW")
            .index("0")
            .parameters(List.of(
                ComponentParameter.builder()
                    .type("payload")
                    .payload(buttonPayload)
                    .build()
            ))
            .build();

        // Montamos el objeto template
        RequestTemplate tpl = RequestTemplate.builder()
            .name(templateName)
            .language(Map.of("code", languageCode))
            .components(List.of(header, body, footer, button))
            .build();

        msg.setTemplate(tpl);
        return msg;
    }

    private static RequestMessages base(String to, String type) {
        RequestMessages msg = new RequestMessages();
        msg.setMessaging_product("whatsapp");
        msg.setRecipient_type("individual");
        msg.setTo(to);
        msg.setType(type);
        return msg;
    }
}

