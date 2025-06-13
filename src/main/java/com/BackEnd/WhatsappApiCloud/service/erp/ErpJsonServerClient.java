package com.BackEnd.WhatsappApiCloud.service.erp;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.core.ParameterizedTypeReference;

import com.BackEnd.WhatsappApiCloud.exception.ErpNotFoundException;
import com.BackEnd.WhatsappApiCloud.exception.ServerClientException;
import com.BackEnd.WhatsappApiCloud.model.dto.erp.ErpUserDto;

@Component
public class ErpJsonServerClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RestClient restClient;
    private final String uriJsonServer;

    public ErpJsonServerClient(
        @Value("${baseurl.jsonserver}") String baseUrlJsonServer,
        @Value("${uri.jsonserver}")     String uriJsonServer
    ) {
        this.uriJsonServer = uriJsonServer;
        this.restClient = RestClient.builder()
            .baseUrl(baseUrlJsonServer)
            .build();

    }

    /**
     * Llama a GET {baseUrlJsonServer}{uriJsonServer}{identificacion} "http://localhost:3001/data?identificacion=0704713619"
     * Espera un array JSON y retorna el primer ErpUserDto encontrado.
     */
    public ErpUserDto getUser(String identificacion) {
        try {
            List<ErpUserDto> users = restClient.get()
                .uri(uriJsonServer + identificacion)
                .retrieve()
                .body(new ParameterizedTypeReference<List<ErpUserDto>>() {});
            
            if (users == null || users.isEmpty()) {
                logger.warn("No se encontró usuario en ERP para cédula {}", identificacion);
                throw new ErpNotFoundException("No se encontró usuario en ERP para identificación: " + identificacion);
            }
            return users.get(0);
            
        } catch (RestClientException e) {
            logger.error("Error al conectar con el ERP: ", e.getMessage());
            throw new ServerClientException("Error al conectar con el ERP: " + e.getMessage(), e);
        }
    }
}