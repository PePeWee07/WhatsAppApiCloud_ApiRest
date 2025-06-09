package com.BackEnd.WhatsappApiCloud.service.glpi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.BackEnd.WhatsappApiCloud.exception.ServerClientException;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.GlpiDto.UserGlpi;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.GlpiDto.UserTicket;

@Component
public class GlpiServerClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${glpi.user_token}")
    String userTokenGlpi;

    private final RestClient apiClient;
    private final RestClient absoluteUriClient;
    private final String appTokenGlpi;
    private String sessionToken = null;
    private Instant sessionCreatedAt = Instant.MIN;
    private static final Duration REFRESH_AFTER = Duration.ofMinutes(21);

    public GlpiServerClient(
            @Value("${baseurl.glpi}") String baseUrlGlpiServer,
            @Value("${glpi.app_token}") String appTokenGlpi) {
        this.apiClient = RestClient.builder()
                .baseUrl(baseUrlGlpiServer)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("App-Token", appTokenGlpi)
                .build();
        this.absoluteUriClient = RestClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("App-Token", appTokenGlpi)
                .build();
        this.appTokenGlpi = appTokenGlpi;
    }

    // ---------- Crear una sesión en GLPI y obtener un token de sesión ----------
    private synchronized String fetchNewSessionToken() {
        try {
            String response = apiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/initSession")
                            .queryParam("App-Token", appTokenGlpi)
                            .queryParam("user_token", userTokenGlpi)
                            .build())
                    .retrieve()
                    .body(String.class);

            String newToken = new JSONObject(response).getString("session_token");
            this.sessionToken = newToken;
            this.sessionCreatedAt = Instant.now();
            System.out.println("Session Token GLPI: " + newToken); // ! Debug
            return newToken;
        } catch (RestClientException ex) {
            logger.error("Error al crear la sesión en GLPI:", ex);
            throw new ServerClientException("No se pudo crear la sesión en GLPI", ex);
        }
    }

    // ---------- Obtener el token de sesión GLPI, renovándolo si es necesario
    // ----------
    public String getSessionTokenGlpi() {
        if (sessionToken == null) {
            return fetchNewSessionToken();
        }
        Instant now = Instant.now();
        if (now.isAfter(sessionCreatedAt.plus(REFRESH_AFTER))) {
            logger.info("Han pasado 21 minutos desde sessionToken {}, renovando...", sessionToken);
            return fetchNewSessionToken();
        }
        return sessionToken;
    }

    // ---------- Obtener el ticket con datos de Solicitante, Observador, Asignado
    // ----------
    public List<UserTicket> getTicketUser(String ticketId) {
        String sessionToken = getSessionTokenGlpi();

        try {
            UserTicket[] response = apiClient.get()
                    .uri("/Ticket/" + ticketId + "/Ticket_User")
                    .header("Session-Token", sessionToken)
                    .retrieve()
                    .body(UserTicket[].class);

            return Arrays.asList(response);
        } catch (RestClientException e) {
            logger.error("Error al obtener ticket del usuario: ", e.getMessage());
            throw new ServerClientException("\"Error al obtener ticket del usuario: ", e.getCause());
        }
    }

    // ---------- Obtener datos del usuario ----------
    public UserGlpi getUserByLink(String urlUser) {
        String sessionToken = getSessionTokenGlpi();

        URI uri = UriComponentsBuilder
                .fromHttpUrl(urlUser)
                .queryParam("expand_dropdowns", true)
                .build()
                .toUri();

        try {
            UserGlpi response = absoluteUriClient.get()
                    .uri(uri)
                    .header("Session-Token", sessionToken)
                    .retrieve()
                    .body(UserGlpi.class);

            return response;
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            String msg = String.format("HTTP %d al obtener usuario: %s", ex.getStatusCode(), body);
            logger.error(msg, ex);
            throw new ServerClientException(msg, ex);

        } catch (RestClientException ex) {
            String msg = "Error genérico al obtener usuario: " + ex.getCause();
            logger.error(msg, ex);
            throw new ServerClientException(msg, ex);
        }
    }

    public String findTicket(String ticketId) {
        String sessionToken = getSessionTokenGlpi();

        try {
            String response = apiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/Ticket/" + ticketId)
                            .queryParam("expand_dropdowns", true)
                            .queryParam("with_notes", true)
                            .queryParam("with_documents", true)
                            .queryParam("with_tickets", true)
                            .build())
                    .header("Session-Token", sessionToken)
                    .retrieve()
                    .body(String.class);

            return response;
        } catch (RestClientException e) {
            logger.error("Error al buscar ticket: ", e.getMessage());
            throw new ServerClientException("Error al buscar ticket: ", e);
        }
    }

    public String TicketWithNotes(String ticketId) {
        String sessionToken = getSessionTokenGlpi();

        try {
            String response = apiClient.get()
                    .uri("/Ticket/" + ticketId + "/ITILFollowup")
                    .header("Session-Token", sessionToken)
                    .retrieve()
                    .body(String.class);

            return response;

        } catch (RestClientException e) {
            logger.error("Error al buscar seguimeinto del ticket: ", e.getMessage());
            throw new ServerClientException("Error al buscar seguimeinto del ticket: ", e);
        }
    }
}
