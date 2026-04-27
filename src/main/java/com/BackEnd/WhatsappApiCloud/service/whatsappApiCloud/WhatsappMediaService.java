package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud;

import java.io.File;
import java.io.IOException;
import org.springframework.lang.Nullable;

import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MediaDownloadResult;

public interface WhatsappMediaService {
    /**
     * Descarga un media de WhatsApp a un archivo temporal local.
     * @param mediaId       id del media devuelto por WA
     * @param filenameHint  nombre sugerido (caption/alias) opcional
     */
    File downloadMediaToTemp(String mediaId, @Nullable String filenameHint) throws IOException;
    String uploadMedia(File mediaFile);
    MediaDownloadResult downloadMediaAsBytes(String mediaId) throws IOException;
}
