package com.BackEnd.WhatsappApiCloud.service.erp;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.MediaType;

import com.BackEnd.WhatsappApiCloud.exception.ErpNotFoundException;
import com.BackEnd.WhatsappApiCloud.exception.ServerClientException;
import com.BackEnd.WhatsappApiCloud.model.dto.erp.ErpUserDto;

@Component
public class ErpServerClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RestClient restClient;
    private final String erpDataUri;
    private final AuthService authService;

    public ErpServerClient(
            @Value("${erp.api.base-url}") String baseUrlJsonServer,
            @Value("${erp.api.data-uri}") String erpDataUri,
            AuthService authService) {
        this.erpDataUri = erpDataUri;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrlJsonServer)
                .build();
        this.authService = authService;
    }

    /**
     * ERP-SERVICE
     */

    public ErpUserDto getUser(String identificacion) {
        try {
            String token = authService.getToken();
            ErpUserDto user = restClient.post()
                    .uri(erpDataUri)
                    .headers(headers -> headers.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("user", identificacion)) 
                    .retrieve()
                    .body(ErpUserDto.class);

            if (user == null || user.getIdentificacion() == null) {
                logger.warn("No se encontró usuario en ERP para cédula {}", identificacion);
                throw new ErpNotFoundException("No se encontró usuario en ERP para identificación: " + identificacion);
            }
            return user;

        } catch (RestClientException e) {
            logger.error("Error al conectar con el ERP: {}", e.getMessage());
            throw new ServerClientException("Error al conectar con el ERP: " + e.getMessage(), e);
        }
    }

    /**
     * JSON SERVER
     */

    // public ErpUserDto getUser(String identificacion) {
    //     try {
    //         List<ErpUserDto> users = restClient.get()
    //                 .uri(erpDataUri + identificacion)
    //                 .retrieve()
    //                 .body(new ParameterizedTypeReference<List<ErpUserDto>>() {
    //                 });

    //         if (users == null || users.isEmpty()) {
    //             logger.warn("No se encontró usuario en ERP para cédula {}", identificacion);
    //             throw new ErpNotFoundException("No se encontró usuario en ERP para identificación: " + identificacion);
    //         }
    //         return users.get(0);

    //     } catch (RestClientException e) {
    //         logger.error("Error al conectar con el ERP: ", e.getMessage());
    //         throw new ServerClientException("Error al conectar con el ERP: " + e.getMessage(), e);
    //     }
    // }
}