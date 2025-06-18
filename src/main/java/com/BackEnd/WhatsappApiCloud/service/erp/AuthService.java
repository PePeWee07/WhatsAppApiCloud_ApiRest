package com.BackEnd.WhatsappApiCloud.service.erp;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import com.BackEnd.WhatsappApiCloud.util.JwtUtils;

@Service
public class AuthService {

    private final RestClient restClient;
    private final String loginUri;
    private final String userBase64;
    private final String passBase64;

    // Token y expiración en memoria
    private volatile String token;
    private volatile Instant expiresAt;

    public AuthService(
            @Value("${erp.api.base-url}") String baseUrl,
            @Value("${erp.api.login-uri}") String loginUri,
            @Value("${erp.api.user-base64}") String userBase64,
            @Value("${erp.api.password-base64}") String passBase64) {
        this.loginUri = loginUri;
        this.userBase64 = userBase64;
        this.passBase64 = passBase64;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    private synchronized void refreshToken() {
        try {
            ResponseEntity<TokenResponse> responseEntity = restClient.post()
                    .uri(loginUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "user", userBase64,
                            "password", passBase64))
                    .retrieve()
                    .toEntity(TokenResponse.class);

            if (responseEntity == null || responseEntity.getBody() == null) {
                throw new RuntimeException("Respuesta inválida al renovar token");
            }

            this.token = responseEntity.getBody().getToken();
            this.expiresAt = JwtUtils.extractExpiration(this.token);

        } catch (RestClientException e) {
            throw new RuntimeException("No se pudo renovar el JWT de ERP-SERVICE ", e);
        }
    }

    /**
     * Devuelve un JWT válido, renovándolo si no existe o está 5min de expirar.
     */
    public synchronized String getToken() {
        if (token == null ||
                Instant.now().isAfter(expiresAt.minus(Duration.ofMinutes(5)))) {
            refreshToken();
        }
        return token;
    }

    // DTO interno para mapear { "token": "..." }
    private static class TokenResponse {
        private String token;

        public String getToken() {
            return token;
        }

        @SuppressWarnings("unused")
        public void setToken(String token) {
            this.token = token;
        }
    }
}
