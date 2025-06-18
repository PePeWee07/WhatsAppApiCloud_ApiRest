package com.BackEnd.WhatsappApiCloud.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public class JwtUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Extrae el Instant de expiración (claim "exp") de un JWT, sin verificar firma.
     *
     * @param jwt token con formato "header.payload.signature"
     * @return Instant de expiración (UTC)
     * @throws IllegalArgumentException si el JWT está mal formado o no contiene "exp"
     */
    public static Instant extractExpiration(String jwt) {
        String[] partes = jwt.split("\\.");
        if (partes.length < 2) {
            throw new IllegalArgumentException("Formato JWT de ERP-SERVICE inválido");
        }
        // decodifica sólo el payload
        byte[] decoded = Base64.getUrlDecoder().decode(partes[1]);
        try {
            JsonNode payload = MAPPER.readTree(new String(decoded, StandardCharsets.UTF_8));
            if (!payload.has("exp")) {
                throw new IllegalArgumentException("El JWT de ERP-SERVICE no contiene claim 'exp'");
            }
            long expSec = payload.get("exp").asLong();
            return Instant.ofEpochSecond(expSec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parseando payload JWT de ERP-SERVICE", e);
        }
    }
}
