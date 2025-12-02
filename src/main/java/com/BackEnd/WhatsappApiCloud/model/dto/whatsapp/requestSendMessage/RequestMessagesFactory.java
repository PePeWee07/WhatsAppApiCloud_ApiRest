package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage;

import java.util.List;
import java.util.Map;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.media.*;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.templates.ComponentParameter;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.templates.RequestTemplate;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.templates.TemplateComponent;

public class RequestMessagesFactory {

    public static RequestMessages buildTextMessage(String to, String message, String contextId) {
        RequestMessages req = base(to, "text", contextId);
        req.setText(new RequestMessageText(false, message));
        return req;
    }

    public static RequestMessages buildImageByUrl(String to, String url, String caption, String contextId) {
        RequestMessages req = base(to, "image", contextId);
        req.setImage(new RequestMediaUrl(url, caption));
        return req;
    }

    public static RequestMessages buildImageById(String to, String mediaId, String caption, String contextId) {
        RequestMessages req = base(to, "image", contextId);
        req.setImage(new RequestMediaId(mediaId, caption));
        return req;
    }

    public static RequestMessages buildVideoByUrl(String to, String url, String caption, String contextId) {
        RequestMessages req = base(to, "video", contextId);
        req.setVideo(new RequestMediaUrl(url, caption));
        return req;
    }

    public static RequestMessages buildVideoById(String to, String mediaId, String caption, String contextId) {
        RequestMessages req = base(to, "video", contextId);
        req.setVideo(new RequestMediaId(mediaId, caption));
        return req;
    }

    public static RequestMessages buildDocumentById(String to, String documentId, String caption, String filename, String contextId) {
        RequestMessages req = base(to, "document", contextId);
        req.setDocument(new RequestDocumentId(documentId, caption, filename));
        return req;
    }

    public static RequestMessages buildDocumentByUrl(String to, String documentUrl, String caption, String filename, String contextId) {
        RequestMessages req = base(to, "document", contextId);
        req.setDocument(new RequestDocumentUrl(documentUrl, caption, filename));
        return req;
    }

    public static RequestMessages buildStickerByUrl(String to, String url, String contextId) {
        RequestMessages req = base(to, "sticker", contextId);
        req.setSticker(new RequestSticker(url));
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
        RequestMessages msg = base(to, "template", null);

        // Header
        TemplateComponent header = TemplateComponent.builder()
            .type("header")
            .parameters(List.of(
                ComponentParameter.builder()
                    .type("image")
                    .image(Map.of("link", headerMediaId))
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

    private static RequestMessages base(String to, String type, String contextId) {
        RequestMessages msg = new RequestMessages();
        msg.setMessaging_product("whatsapp");
        msg.setRecipient_type("individual");
        msg.setTo(to);
        msg.setType(type);
        if (contextId != null && !contextId.isBlank()) {
            msg.setContext(new RequestContext(contextId));
        }
        return msg;
    }
}

