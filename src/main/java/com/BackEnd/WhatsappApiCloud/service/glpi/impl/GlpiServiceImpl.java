package com.BackEnd.WhatsappApiCloud.service.glpi.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import java.io.File;
import java.nio.file.Files;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;


import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.BackEnd.WhatsappApiCloud.exception.ServerClientException;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.GlpiDto.*;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.TicketInfoDto;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.TicketInfoDto.*;
import com.BackEnd.WhatsappApiCloud.model.entity.glpi.UserTicketEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatEntity;
import com.BackEnd.WhatsappApiCloud.repository.UserChatRepository;
import com.BackEnd.WhatsappApiCloud.repository.UserTicketRepository;
import com.BackEnd.WhatsappApiCloud.service.glpi.GlpiServerClient;
import com.BackEnd.WhatsappApiCloud.service.glpi.GlpiService;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.ApiWhatsappService;

@Service
public class GlpiServiceImpl implements GlpiService {

        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final GlpiServerClient glpiServerClient;

        private final UserChatRepository userChatRepository;
        private final UserTicketRepository userTicketRepository;

        private final ApiWhatsappService apiWhatsappService;

        public GlpiServiceImpl(GlpiServerClient glpiServerClient, ApiWhatsappService apiWhatsappService, 
                        UserChatRepository userChatRepository, UserTicketRepository userTicketRepository) {
                                this.apiWhatsappService = apiWhatsappService;
                                this.glpiServerClient = glpiServerClient;
                                this.userChatRepository = userChatRepository;
                                this.userTicketRepository = userTicketRepository;
        }

        
        private List<String> extractMediaIdsFromLinks(Link[] links) {
        List<String> mediaIds = new ArrayList<>();
        Arrays.stream(links)
                .filter(link -> "Document_Item".equals(link.rel()))
                .forEach(link -> {
                        List<Document_Item> documentItems = glpiServerClient.getDocumentItems(link.href());
                        documentItems.forEach(documentItem -> {
                        Arrays.stream(documentItem.links())
                                .filter(docLink -> "Document".equals(docLink.rel()))
                                .forEach(docLink -> {
                                        try {
                                        byte[] fileData = glpiServerClient.downloadDocument(docLink.href());

                                        Tika tika = new Tika();
                                        String contentType = tika.detect(fileData);

                                        if (!isMimeTypeAllowed(contentType)) {
                                                return;
                                        }

                                        String extension = getExtensionFromDocumentId(documentItem.documents_id());

                                        File tempFile = File.createTempFile("document", extension);
                                        Files.write(tempFile.toPath(), fileData);

                                        String mediaId = apiWhatsappService.uploadMedia(tempFile);
                                        mediaIds.add(mediaId);

                                        tempFile.delete();
                                        } catch (Exception e) {
                                        logger.error("Error procesando documento del GLPI: " + e.getMessage(), e);
                                        throw new ServerClientException("Error procesar documento del GLPI: " + e.getMessage(), e);
                                        }
                                });
                        });
                });
        return mediaIds;
        }

        private String getExtensionFromDocumentId(String documentsId) {
                int lastDotIndex = documentsId.lastIndexOf('.');
                if (lastDotIndex != -1 && lastDotIndex < documentsId.length() - 1) {
                        return documentsId.substring(lastDotIndex);
                }
                return ".tmp";
        }

        private boolean isMimeTypeAllowed(String mimeType) {
                List<String> allowedMimeTypes = Arrays.asList(
                                "audio/aac", "audio/mp4", "audio/mpeg", "audio/amr", "audio/ogg", "audio/opus",
                                "application/vnd.ms-powerpoint", "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/pdf", "text/plain", "text/csv", "application/vnd.ms-excel",
                                "image/jpeg", "image/png", "image/webp", "video/mp4", "video/3gpp");
                return allowedMimeTypes.contains(mimeType);
        }

        private String cleanHtmlForWhatsApp(String rawHtml) {
                // 1) Parseamos y decodificamos entidades
                Document doc = Jsoup.parse(Parser.unescapeEntities(rawHtml, false));
                doc.outputSettings().prettyPrint(false);

                StringBuilder sb = new StringBuilder();

                // 2) Recorremos sólo los hijos directos de <body>
                for (Node node : doc.body().childNodes()) {
                        if (node instanceof Element) {
                        Element el = (Element) node;
                        switch (el.tagName()) {
                                case "p":
                                // párrafos TOP-level
                                appendParagraph(sb, el.text());
                                break;
                                case "div":
                                // tu contenedor de contenido GLPi
                                if (el.hasClass("item_content")) {
                                        Element rich = el.selectFirst("div.rich_text_container");
                                        if (rich != null) {
                                        // 2.1) Dentro, sólo los <p> directos
                                        for (Element p : rich.select("> p")) {
                                                appendParagraph(sb, p.text());
                                        }
                                        // 2.2) Después, sólo el <ul> para los ítems
                                        Element ul = rich.selectFirst("ul");
                                        if (ul != null) {
                                                for (Element li : ul.select("> li")) {
                                                sb.append("* ").append(li.text().trim()).append("\n");
                                                }
                                                sb.append("\n");
                                        }
                                        }
                                }
                                break;
                        }
                        } else if (node instanceof TextNode) {
                        // texto suelto fuera de etiquetas
                        String t = ((TextNode) node).text().trim();
                        appendParagraph(sb, t);
                        }
                }

                return sb.toString().trim();
        }

        private void appendParagraph(StringBuilder sb, String text) {
                if (text != null && !text.isEmpty()) {
                        sb.append(text).append("\n\n");
                }
        }


        @Override
        public TicketInfoDto getInfoTicketById(String ticketId) {
                List<UserTicket> userTickets = glpiServerClient.getTicketUser(ticketId);

                // 1) Obtener link del ticket desde el solicitante
                String ticketLink = userTickets.stream()
                                .filter(t -> t.type() == 1)
                                .flatMap(t -> Arrays.stream(t.links()))
                                .filter(link -> "Ticket".equalsIgnoreCase(link.rel()))
                                .map(link -> link.href())
                                .findFirst()
                                .orElseThrow(() -> new ServerClientException("No se encontró el enlace del ticket."));

                // 2) Solicitante (type=1)
                String requester = userTickets.stream()
                                .filter(t -> t.type() == 1)
                                .map(UserTicket::alternative_email)
                                .findFirst()
                                .orElse("—sin solicitante—");

                // 3) Observadores (type=3)
                List<String> watchers = userTickets.stream()
                                .filter(t -> t.type() == 3)
                                .map(UserTicket::alternative_email)
                                .collect(Collectors.toList());

                // 4) Técnicos asignados (type=2)
                List<TechDto> techDtos = userTickets.stream()
                                .filter(t -> t.type() == 2)
                                .map(ut -> {
                                        String userHref = Arrays.stream(ut.links())
                                                        .filter(link -> "User".equals(link.rel()))
                                                        .map(link -> link.href())
                                                        .findFirst()
                                                        .orElseThrow(
                                                                        () -> new ServerClientException(
                                                                                        "No se encontró el enlace del usuario técnico."));
                                        UserGlpi tech = glpiServerClient.getUserByLink(userHref);
                                        return new TechDto(
                                                        tech.id(),
                                                        tech.name(),
                                                        tech.mobile(),
                                                        tech.realname(),
                                                        tech.firstname(),
                                                        tech.locations_id(),
                                                        tech.is_active(),
                                                        tech.profiles_id(),
                                                        tech.usertitles_id(),
                                                        tech.groups_id(),
                                                        tech.users_id_supervisor());
                                })
                                .collect(Collectors.toList());

                // 5) Datos del ticket
                Ticket glpiTicket = glpiServerClient.getTicketByLink(ticketLink);
                TicketDto ticketDto = new TicketDto(
                glpiTicket.id(),
                glpiTicket.name(),
                glpiTicket.closedate(),
                glpiTicket.solvedate(),
                glpiTicket.date_mod(),
                glpiTicket.status() == 1L ? "Nuevo"
                        : glpiTicket.status() == 2L ? "En curso (Asignado)"
                                : glpiTicket.status() == 3L ? "En curso (Planificado)"
                                        : glpiTicket.status() == 4L ? "En espera"
                                                : glpiTicket.status() == 5L ? "Resuelto"
                                                        : glpiTicket.status() == 6L ? "Cerrado" : "Indefinido",
                cleanHtmlForWhatsApp(glpiTicket.content()),
                glpiTicket.urgency() == 1L ? "Muy baja"
                        : glpiTicket.urgency() == 2L ? "Baja"
                                : glpiTicket.urgency() == 3L ? "Media"
                                        : glpiTicket.urgency() == 4L ? "Alta"
                                                : glpiTicket.urgency() == 5L ? "Muy Alta"
                                                        : glpiTicket.urgency() == 6L ? "Primordial" : "Indefinido",
                glpiTicket.impact() == 1L ? "Muy baja"
                        : glpiTicket.impact() == 2L ? "Baja"
                                : glpiTicket.impact() == 3L ? "Media"
                                        : glpiTicket.impact() == 4L ? "Alta"
                                                : glpiTicket.impact() == 5L ? "Muy Alta"
                                                        : glpiTicket.impact() == 6L ? "Primordial" : "Indefinido",
                glpiTicket.priority() == 1L ? "Muy baja"
                        : glpiTicket.priority() == 2L ? "Baja"
                                : glpiTicket.priority() == 3L ? "Media"
                                        : glpiTicket.priority() == 4L ? "Alta"
                                                : glpiTicket.priority() == 5L ? "Muy Alta"
                                                        : glpiTicket.priority() == 6L ? "Primordial" : "Indefinido",
                glpiTicket.itilcategories_id(),
                glpiTicket.type() == 1L ? "Incidencia" : glpiTicket.type() == 2L ? "Solicitud" : "Indefinido",
                glpiTicket.locations_id(),
                glpiTicket.date_creation());

                // 6) Si fue solucionado
                List<TicketSolutionDto> solutionDto = new ArrayList<>();
                if (glpiTicket.solvedate() != null) {
                        List<TicketSolution> glpiSolutions = glpiServerClient.getTicketSolution(glpiTicket.id());
                       solutionDto = glpiSolutions.stream()
                        .map(solution -> {
                                String formatted = cleanHtmlForWhatsApp(solution.content());

                                String solutionStatus = solution.status() == 1L ? "None"
                                                : solution.status() == 2L ? "En espera"
                                                : solution.status() == 3L ? "Aceptado"
                                                : solution.status() == 4L ? "Rechazado"
                                                : "Indefinido";

                                List<String> mediaIds = extractMediaIdsFromLinks(solution.links());

                                return new TicketSolutionDto(
                                formatted,
                                solution.date_creation(),
                                solutionStatus,
                                mediaIds.isEmpty() ? null : mediaIds
                                );
                        })
                        .collect(Collectors.toList());
                }

                // 7) Seguimientos (_notes)
                List<TicketFollowUp> rawNotes = glpiServerClient.TicketWithNotes(glpiTicket.id());
                List<NoteDto> notes = rawNotes.stream()
                        .map(n -> {
                        String content = cleanHtmlForWhatsApp(n.content());
                        List<String> mediaIds = extractMediaIdsFromLinks(n.links());

                        return new NoteDto(
                                n.date_creation(),
                                content,
                                mediaIds.isEmpty() ? null : mediaIds
                                );
                        }).collect(Collectors.toList());

                // 8) Construir y devolver DTO final
                return new TicketInfoDto(
                                requester,
                                watchers,
                                techDtos,
                                ticketDto,
                                solutionDto,
                                notes);
        }

        @Override
        @Transactional
        public responseCreateTicketSuccess createTicket(CreateTicket payload, String whatsAppPhone) {

                // 1) Verificar que el usuario existe
                UserChatEntity user = userChatRepository
                        .findByWhatsappPhone(whatsAppPhone)
                        .orElseThrow(() -> new ServerClientException(
                                "Usuario no encontrado para el número de WhatsApp: " + whatsAppPhone));

                // 2) Construir el cuerpo del ticket con los datos proporcionados
                CreateTicket ticketToCreate = new CreateTicket(
                        new Input(
                                payload.input().name(),
                                payload.input().content(),
                                payload.input().entities_id(),
                                payload.input().requesttypes_id(),
                                payload.input()._users_id_requester(),
                                new UserIdRequesterNotif(
                                        payload.input()._users_id_requester_notif().use_notification(),
                                        payload.input()._users_id_requester_notif().alternative_email()
                                ),
                                payload.input().users_id_lastupdater()
                        )
                );

                // 3) Crear el ticket en GLPI
                responseCreateTicketSuccess response = glpiServerClient.createTicket(ticketToCreate);

                // 4) Obtener los datos del ticket creado
                Ticket glpiTicket = glpiServerClient.getTicketById(response.id());

                // 5) Guardar el ticket en la base de datos local
                UserTicketEntity entity = new UserTicketEntity();
                entity.setId(glpiTicket.id());
                entity.setWhatsappPhone(user.getWhatsappPhone());
                entity.setName(glpiTicket.name());
                entity.setStatus(
                        switch (glpiTicket.status().intValue()) {
                                case 1 -> "Nuevo";
                                case 2 -> "En curso (Asignado)";
                                case 3 -> "En curso (Planificado)";
                                case 4 -> "En espera";
                                case 5 -> "Resuelto";
                                case 6 -> "Cerrado";
                                default -> "Indefinido";
                        }
                );
                entity.setDate_creation(glpiTicket.date_creation());
                entity.setClosedate(glpiTicket.closedate());
                entity.setSolvedate(glpiTicket.solvedate());
                entity.setDate_mod(glpiTicket.date_mod());

                userTicketRepository.save(entity);

                return response;
        }
}
