package com.BackEnd.WhatsappApiCloud.service.erp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.BackEnd.WhatsappApiCloud.model.dto.erp.ErpUserDto;

/**
 * Servicio intermedio que primero intenta leer de la caché "erpUserCache".
 * Si no existe, llama al ErpJsonServerClient y guarda el resultado en Redis.
 */
@Service
public class ErpCacheService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ErpJsonServerClient erpClient;

    public ErpCacheService(ErpJsonServerClient erpClient) {
        this.erpClient = erpClient;
    }

    /**
     * @param identificacion clave única de usuario en el ERP (cedula)
     * @return ErpUserDto recuperado del ERP o de la caché Redis
     */
    @Cacheable(cacheNames = "erpUserCache", key = "#identificacion")
    public ErpUserDto getCachedUser(String identificacion) {
        logger.debug("Cache miss para ERP: {}", identificacion);
        ErpUserDto dto = erpClient.getUser(identificacion);
        if (dto == null || dto.getIdentificacion() == null) {
            logger.warn("ERP devolvió null o identificación nula para {}", identificacion);
        }
        return dto;
    }
}
