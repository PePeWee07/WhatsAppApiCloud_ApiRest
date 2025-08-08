package com.BackEnd.WhatsappApiCloud.service.glpi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.BackEnd.WhatsappApiCloud.exception.GlpiNotFoundException;
import com.BackEnd.WhatsappApiCloud.exception.ServerClientException;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.GlpiDto.*;

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
    private final ObjectMapper objectMapper;

    public GlpiServerClient(
            @Value("${baseurl.glpi}") String baseUrlGlpiServer,
            @Value("${glpi.app_token}") String appTokenGlpi,
            ObjectMapper objectMapper) {
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
        this.objectMapper = objectMapper;
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

    // ----- Obtener el token de sesión GLPI, renovándolo si es necesario -----
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

    // ----- Obtener el ticket con datos de Solicitante, Observador, Asignado -----
    public List<UserTicket> getTicketUser(String ticketId) {
        String sessionToken = getSessionTokenGlpi();

        try {
            UserTicket[] response = apiClient.get()
                    .uri("/Ticket/" + ticketId + "/Ticket_User")
                    .header("Session-Token", sessionToken)
                    .retrieve()
                    .body(UserTicket[].class);

            return Arrays.asList(response);
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            if (ex.getStatusCode().value() == 404) {
                throw new GlpiNotFoundException("No se encontró información de usuarios para el ticket GLPI con ID: " + ticketId);
            }
            String msg = String.format("HTTP %d al obtener información de usuarios del ticket: %s", ex.getStatusCode().value(), body);
            logger.error(msg, ex);
            throw new ServerClientException(msg, ex);

        } catch (RestClientException ex) {
            logger.error("Error genérico al obtener información de usuarios del ticket: " + ex.getMessage(), ex);
            throw new ServerClientException("Error genérico al obtener información de usuarios del ticket: " + ex.getMessage(), ex);
        }
    }

    // ----- Obtener el email de un usuario de la base de datos del GLPI -----
    public List<Usermail> getEmailUser(Long userId) {
        String sessionToken = getSessionTokenGlpi();

        try {
            Usermail[] response = apiClient.get()
                    .uri("/User/" + userId + "/Useremail")
                    .header("Session-Token", sessionToken)
                    .retrieve()
                    .body(Usermail[].class);

            return Arrays.asList(response);
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            if (ex.getStatusCode().value() == 404) {
                throw new GlpiNotFoundException("No se encontró email del usuario: " + userId);
            }
            String msg = String.format("No se encontró email del usuario: ", ex.getStatusCode().value(), body);
            logger.error(msg, ex);
            throw new ServerClientException(msg, ex);

        } catch (RestClientException ex) {
            logger.error("Error genérico al obtener email de usuario: " + ex.getMessage(), ex);
            throw new ServerClientException("Error genérico al obtener email de usuario: " + ex.getMessage(), ex);
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
            if (ex.getStatusCode().value() == 404) {
                throw new GlpiNotFoundException("No se encontró el Usuario en el GLPI con url: " + urlUser);
            }
            String msg = String.format("HTTP %d al obtener usuario: %s", ex.getStatusCode(), body);
            logger.error(msg, ex);
            throw new ServerClientException(msg, ex);

        } catch (RestClientException ex) {
            logger.error("Error genérico al obtener usuario: " + ex.getMessage(), ex);
            throw new ServerClientException("Error genérico al obtener usuario: " + ex.getMessage(), ex);
        }
    }

    // ---------- Obtener Ticket ----------
    public Ticket getTicketByLink(String ticketUrl) {
        String sessionToken = getSessionTokenGlpi();

        URI uri = UriComponentsBuilder
                .fromHttpUrl(ticketUrl)
                .queryParam("expand_dropdowns", true)
                .queryParam("with_notes", true)
                .queryParam("with_documents", true)
                .queryParam("with_tickets", true)
                .build()
                .toUri();

        try {
            Ticket response = absoluteUriClient.get()
                    .uri(uri)
                    .header("Session-Token", sessionToken)
                    .retrieve()
                    .body(Ticket.class);

            return response;
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            if (ex.getStatusCode().value() == 404) {
                throw new GlpiNotFoundException("No se encontró el Ticket en el GLPI con url: " + ticketUrl);
            }
            String msg = String.format("HTTP %d al obtener Ticket: %s", ex.getStatusCode(), body);
            logger.error(msg, ex);
            throw new ServerClientException(msg, ex);
        } catch (RestClientException e) {
            logger.error("Error genérico al obtener el ticket: " + e.getMessage(), e);
            throw new ServerClientException("Error genérico al obtener el ticket: " + e.getMessage(), e);
        }
    }

    // ---------- Obtener Ticket por ID ----------
    public Ticket getTicketById(Long ticketid) {
        String sessionToken = getSessionTokenGlpi();

        try {
            Ticket response = apiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/Ticket/" + ticketid)
                            .queryParam("expand_dropdowns", true)
                            .build())
                    .header("Session-Token", sessionToken)
                    .retrieve()
                    .body(Ticket.class);

            return response;
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            if (ex.getStatusCode().value() == 404) {
                throw new GlpiNotFoundException("Ticket no encontrado en GLPI con ID: " + ticketid);
            }
            String msg = String.format("HTTP %d al obtener Ticket por Id: %s", ex.getStatusCode(), body);
            logger.error(msg, ex);
            throw new ServerClientException(msg, ex);
        } catch (RestClientException e) {
            logger.error("Error genérico al obtener el ticket por Id: " + e.getMessage(), e);
            throw new ServerClientException("Error genérico al obtener el ticket por Id: " + e.getMessage(), e);
        }
    }

    // ---------- Obtener seguimientos de un ticket ----------
    public List<TicketFollowUp> TicketWithNotes(Long ticketId) {
        String sessionToken = getSessionTokenGlpi();

        try {
            TicketFollowUp[] response = apiClient.get()
                    .uri("/Ticket/" + ticketId + "/ITILFollowup")
                    .header("Session-Token", sessionToken)
                    .retrieve()
                    .body(TicketFollowUp[].class);

            return Arrays.asList(response);

        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            if (ex.getStatusCode().value() == 404) {
                throw new GlpiNotFoundException("No se encontró información Seguimeintos del ticket GLPI con ID: " + ticketId);
            }
            String msg = String.format("HTTP %d al obtener seguimeintos: %s", ex.getStatusCode(), body);
            logger.error(msg, ex);
            throw new ServerClientException(msg, ex);
        } catch (RestClientException e) {
            logger.error("Error genérico al obtener seguimeintos: " + e.getMessage(), e);
            throw new ServerClientException("Error genérico al obtener seguimeintos: " + e.getMessage(), e);
        }
    }

    // ------- Obtener documentos_itens de un seguimeinto ----------
    public List<Document_Item> getDocumentItems(String document_itemUrl) {
        String sessionToken = getSessionTokenGlpi();

        URI uri = UriComponentsBuilder
                .fromHttpUrl(document_itemUrl)
                .queryParam("expand_dropdowns", true)
                .build()
                .toUri();

        try {
            Document_Item[] response = apiClient.get()
                    .uri(uri)
                    .header("Session-Token", sessionToken)
                    .retrieve()
                    .body(Document_Item[].class);

            return Arrays.asList(response);
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            if (ex.getStatusCode().value() == 404) {
                throw new GlpiNotFoundException("No se encontró el docuemtos_itens en el GLPI con url: " + document_itemUrl);
            }
            String msg = String.format("HTTP %d al obtener documentos_itens: %s", ex.getStatusCode(), body);
            logger.error(msg, ex);
            throw new ServerClientException(msg, ex);
        } catch (RestClientException e) {
            logger.error("Error genérico al obtener documentos_itens: " + e.getMessage(), e);
            throw new ServerClientException("Error genérico al obtener documentos_itens: " + e.getMessage(), e);
        }
    }

    // ---------- Descargar Docuemnt_itens por link ----------
    public byte[] downloadDocument(String documentHref) {
        String session = getSessionTokenGlpi();

        URI uri = UriComponentsBuilder
            .fromHttpUrl(documentHref)
            .queryParam("alt", "media")
            .build()
            .toUri();

        try {
            return absoluteUriClient.get()
                .uri(uri)
                .header("Session-Token", session)
                .retrieve()
                .body(byte[].class);
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            if (ex.getStatusCode().value() == 404) {
                throw new GlpiNotFoundException("Documento no encontrado en GLPI con URL: " + documentHref);
            }
            String msg = String.format("HTTP %d al descargar documento: %s", ex.getStatusCode(), body);
            logger.error(msg, ex);
            throw new ServerClientException(msg, ex);
        } catch (RestClientException e) {
            logger.error("Error genérico al descargar documento: " + e.getMessage(), e);
            throw new ServerClientException("Error genérico al descargar documento: " + e.getMessage(), e);
        }
    }

    // ---------- Descargar Docuemnt_itens por id ----------
    public byte[] downloadDocumentById(Long docId) {
        String session = getSessionTokenGlpi();

        try {
            return apiClient.get()
                .uri(uriBuilder -> uriBuilder
                            .path("/Document/" + docId)
                            .queryParam("alt", "media")
                            .build())
                .header("Session-Token", session)
                .retrieve()
                .body(byte[].class);
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            if (ex.getStatusCode().value() == 404) {
                throw new GlpiNotFoundException("Documento no encontrado en GLPI con URL: " + docId);
            }
            String msg = String.format("HTTP %d al descargar documento: %s", ex.getStatusCode(), body);
            logger.error(msg, ex);
            throw new ServerClientException(msg, ex);
        } catch (RestClientException e) {
            logger.error("Error genérico al descargar documento: " + e.getMessage(), e);
            throw new ServerClientException("Error genérico al descargar documento: " + e.getMessage(), e);
        }
    }

    // ---------- Obtener solucion del Ticket ----------
    public List<TicketSolution> getTicketSolutionById(Long ticketId) {
        String sessionToken = getSessionTokenGlpi();

        try {
            TicketSolution[] response = apiClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/Ticket/" + ticketId + "/ITILSolution")
                            .queryParam("expand_dropdowns", true)
                            .build())
                    .header("Session-Token", sessionToken)
                    .retrieve()
                    .body(TicketSolution[].class);

            return Arrays.asList(response);
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            if (ex.getStatusCode().value() == 404) {
                throw new GlpiNotFoundException("Solución de ticket no encontrada en GLPI para ID: " + ticketId);
            }
            String msg = String.format("HTTP %d al obtener solución del ticket: %s", ex.getStatusCode(), body);
            logger.error(msg, ex);
            throw new ServerClientException(msg, ex);
        } catch (RestClientException e) {
            logger.error("Error genérico al obtener solución del ticket: " + e.getMessage(), e);
            throw new ServerClientException("Error genérico al obtener solución del ticket: " + e.getMessage(), e);
        }
    }

    // ---------- Crear Ticket ----------
    public responseCreateTicketSuccess createTicket(CreateTicket ticket) {
        String sessionToken = getSessionTokenGlpi();

        try {
            responseCreateTicketSuccess response = apiClient.post()
                    .uri("/Ticket")
                    .header("Session-Token", sessionToken)
                    .body(ticket)
                    .retrieve()
                    .body(responseCreateTicketSuccess.class);

            return response;
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            String msg = String.format("HTTP %d al crear el ticket: %s", ex.getStatusCode(), body);
            logger.error(msg, ex);
            throw new ServerClientException(msg, ex);
        } catch (RestClientException e) {
            logger.error("Error genérico al crear el ticket: " + e.getMessage(), e);
            throw new ServerClientException("Error genérico al crear el ticket: " + e.getMessage(), e);
        }
    }
   
    // ---------- Edit Status Ticket ----------
    public Object updateTicketStatusById(Long ticketid, RequestUpdateStatus ticket) {
        String sessionToken = getSessionTokenGlpi();

        try {
            Object response = apiClient.put()
                    .uri("/Ticket/" + ticketid)
                    .header("Session-Token", sessionToken)
                    .body(ticket)
                    .retrieve()
                    .body(Object.class);

            return response;
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            if (ex.getStatusCode().value() == 404) {
                throw new GlpiNotFoundException("Ticket no encontrado en GLPI con ID: " + ticketid);
            }
            String msg = String.format("HTTP %d al editar status del Ticket por Id: %s", ex.getStatusCode(), body);
            logger.error(msg, ex);
            throw new ServerClientException(msg, ex);
        } catch (RestClientException e) {
            logger.error("Error genérico al editar status del ticket por Id: " + e.getMessage(), e);
            throw new ServerClientException("Error genérico al editar status del ticket por Id: " + e.getMessage(), e);
        }
    }

    // ---------- Edit Status Ticket Solution (No usada Glpi maneja logica rechazo al retroceder el estado del ticket) ----------
    public Object updateTicketSolutionById(Long solutionId, RequestUpdateStatus ticket) {
        String sessionToken = getSessionTokenGlpi();

        try {
            Object response = apiClient.put()
                    .uri("/ITILSolution/" + solutionId)
                    .header("Session-Token", sessionToken)
                    .body(ticket)
                    .retrieve()
                    .body(Object.class);

            return response;
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            if (ex.getStatusCode().value() == 404) {
                throw new GlpiNotFoundException("Solución de ticket no encontrada en GLPI para ID: " + solutionId);
            }
            String msg = String.format("HTTP %d al editar solución del ticket: %s", ex.getStatusCode(), body);
            logger.error(msg, ex);
            throw new ServerClientException(msg, ex);
        } catch (RestClientException e) {
            logger.error("Error genérico al editar solución del ticket: " + e.getMessage(), e);
            throw new ServerClientException("Error genérico al editar solución del ticket: " + e.getMessage(), e);
        }
    }

    // ---------- Crear un seguimiento al Ticket ----------
    public Object createNoteForTicket(CreateNoteForTicket note) {
        String sessionToken = getSessionTokenGlpi();

        try {
            Object response = apiClient.post()
                    .uri("/TicketFollowup")
                    .header("Session-Token", sessionToken)
                    .body(note)
                    .retrieve()
                    .body(Object.class);
            return response;
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            if (ex.getStatusCode().value() == 404) {
                throw new GlpiNotFoundException("Ticket no encontrado en GLPI para crear seguimiento: " + note);
            }
            String msg = String.format("HTTP %d al crear seguimiento del ticket: %s", ex.getStatusCode(), body);
            logger.error(msg, ex);
            throw new ServerClientException(msg, ex);
        } catch (RestClientException e) {
            logger.error("Error genérico al crear seguimiento del ticket: " + e.getMessage(), e);
            throw new ServerClientException("Error genérico al crear seguimiento del ticket: " + e.getMessage(), e);
        }
    }

    // ---------- Cargar documentos ----------
    public UploadDocumentResponse uploadDocument(File file, String displayName, Integer entitiesId) {
        // 0) Prechequeos
        if (file == null || !file.exists() || file.length() == 0) {
            throw new IllegalArgumentException("Archivo vacío o inexistente");
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = file.getName(); // fallback
        }

        String sessionToken = getSessionTokenGlpi();

        try {
            // 1) Construir manifest (JSON como String)
            var req = new UploadDocumentRequest(displayName, List.of(displayName), entitiesId);
            String manifestJson = objectMapper.writeValueAsString(new UploadDocumentManifest(req));

            // 2) Multipart/form-data
            var form = new org.springframework.util.LinkedMultiValueMap<String, Object>();
            form.add("uploadManifest", manifestJson);
            form.add("filename[0]", new org.springframework.core.io.FileSystemResource(file));

            // 3) POST /Document
            UploadDocumentResponse resp = apiClient.post()
                .uri("/Document") // asegúrate que baseurl.glpi termina en /apirest.php
                .header("Session-Token", sessionToken)
                .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .body(UploadDocumentResponse.class);

            // 4) Validar que GLPI devolvió id válido
            if (resp == null || resp.id() <= 0) {
                throw new IllegalStateException("Respuesta inválida de GLPI /Document");
            }
            return resp;

        } catch (org.springframework.web.client.RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            String msg = "HTTP %d al subir documento: %s".formatted(ex.getStatusCode().value(), body);
            logger.error(msg, ex);
            throw new ServerClientException(msg, ex);

        } catch (Exception e) {
            logger.error("Error genérico al subir documento: " + e.getMessage(), e);
            throw new ServerClientException("Error genérico al subir documento: " + e.getMessage(), e);
        }
    }


    public LinkDocumentItemResponse linkDocumentToTicket(long documentsId, long ticketId) {
        String sessionToken = getSessionTokenGlpi();

        var req = new LinkDocumentItemRequest(
                new LinkDocumentItemRequest.Input("Ticket", ticketId, documentsId)
        );

        try {
            LinkDocumentItemResponse resp = apiClient.post()
                    .uri("/Document_Item") // asegúrate que baseurl.glpi termina en /apirest.php
                    .header("Session-Token", sessionToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(LinkDocumentItemResponse.class);

            if (resp == null || resp.id() <= 0) {
                throw new IllegalStateException("Respuesta inválida de GLPI /Document_Item");
            }
            return resp;

        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            String msg = "HTTP %d al enlazar documento: %s"
                    .formatted(ex.getStatusCode().value(), body);
            logger.error(msg, ex);
            throw new ServerClientException(msg, ex);

        } catch (RestClientException e) {
            logger.error("Error genérico al enlazar documento: " + e.getMessage(), e);
            throw new ServerClientException("Error genérico al enlazar documento: " + e.getMessage(), e);
        }
    }

}
