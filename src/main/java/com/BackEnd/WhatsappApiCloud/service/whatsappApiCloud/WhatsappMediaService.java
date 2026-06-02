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

    /** Sube un media a WhatsApp SIN validación estricta (uso interno, p. ej. reenvío desde GLPI que ya pre-filtra). */
    String uploadMedia(File mediaFile);

    /**
     * Sube un media a WhatsApp validando MIME y tamaño contra la whitelist (doc/img/video) antes de enviar;
     * lanza 400 si no cumple. Usado por la subida del backoffice.
     */
    String uploadValidatedMedia(File mediaFile);

    MediaDownloadResult downloadMediaAsBytes(String mediaId) throws IOException;
}
