package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.media.*;

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

    private static RequestMessages base(String to, String type) {
        RequestMessages msg = new RequestMessages();
        msg.setMessaging_product("whatsapp");
        msg.setRecipient_type("individual");
        msg.setTo(to);
        msg.setType(type);
        return msg;
    }
}

