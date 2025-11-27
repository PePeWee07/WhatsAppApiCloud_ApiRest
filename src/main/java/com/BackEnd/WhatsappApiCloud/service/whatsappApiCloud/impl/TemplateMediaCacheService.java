package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.WhatsappMediaService;

@Service
public class TemplateMediaCacheService {

    private final WhatsappMediaService whatsappMediaService;

    private static final String TEMPLATE_IMAGE_CLASSPATH = "templates/catia_feedback.png";

    public TemplateMediaCacheService(WhatsappMediaService whatsappMediaService) {
        this.whatsappMediaService = whatsappMediaService;
    }

    @Cacheable(cacheNames = "mediaIdCache", key = "#templateName")
    public String loadTemplateMediaId(String templateName) {
        File file = getTemplateImageFile();
        return whatsappMediaService.uploadMedia(file);
    }

    @CacheEvict(cacheNames = "mediaIdCache", key = "#templateName")
    public void evictTemplateMediaId(String templateName) {
        // Solo limpia. El siguiente loadTemplateMediaId() volverá a subir y cachear.
    }

    private File getTemplateImageFile() {
        try {
            Resource res = new ClassPathResource(TEMPLATE_IMAGE_CLASSPATH);

            try (InputStream is = res.getInputStream()) {
                String ext = ".png";
                File tmp = File.createTempFile("tpl_", ext);
                Files.copy(is, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return tmp;
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo cargar la plantilla de feedback desde classpath: " + TEMPLATE_IMAGE_CLASSPATH,
                    e);
        }
    }
}
