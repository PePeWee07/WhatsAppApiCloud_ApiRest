package com.BackEnd.WhatsappApiCloud.service.glpi.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.tika.Tika;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.BackEnd.WhatsappApiCloud.exception.ServerClientException;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.GlpiDto.*;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.SolutionDecisionRequest;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.TicketInfoDto;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.TicketInfoDto.*;
import com.BackEnd.WhatsappApiCloud.model.entity.glpi.UserTicketEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatEntity;
import com.BackEnd.WhatsappApiCloud.repository.UserChatRepository;
import com.BackEnd.WhatsappApiCloud.repository.UserTicketRepository;
import com.BackEnd.WhatsappApiCloud.service.glpi.GlpiServerClient;
import com.BackEnd.WhatsappApiCloud.service.glpi.GlpiService;
import com.BackEnd.WhatsappApiCloud.service.glpi.HtmlCleaner;
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
        
        private List<MediaFileDto> extractMediaIdsFromLinks(Link[] links) {
        List<MediaFileDto> mediaFiles = new ArrayList<>();
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
                                                        mediaFiles.add(new MediaFileDto("Error", documentItem.documents_id(), contentType));
                                                        return;
                                                }

                                                String extension = getExtensionFromDocumentId(documentItem.documents_id());

                                                File tempFile = File.createTempFile("document", extension);
                                                Files.write(tempFile.toPath(), fileData);

                                                String mediaId = apiWhatsappService.uploadMedia(tempFile);
                                                mediaFiles.add(new TicketInfoDto.MediaFileDto(mediaId, documentItem.documents_id(), contentType));

                                                tempFile.delete();
                                        } catch (Exception e) {
                                                logger.error("Error procesando documento del GLPI: "+ documentItem + e.getMessage(), e);
                                                mediaFiles.add(new TicketInfoDto.MediaFileDto("Error", "unknown", "unknown"));
                                        }
                                });
                        });
                });
                return mediaFiles;
        }

        // Filtra los documentos de un ticket
        private static final DateTimeFormatter GLPI_DATETIME_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        public List<MediaFileDto> filterDocuments(
            List<DocumentGlpi> docs,
            String targetUserId,
            String referenceDateStr
        ) {
                LocalDateTime ref = LocalDateTime.parse(referenceDateStr, GLPI_DATETIME_FMT);
                LocalDateTime before = ref.minusMinutes(5);
                LocalDateTime after  = ref.plusMinutes(5);

                return docs.stream()
                .filter(doc -> {
                        String docUser = doc.users_id() == null
                        ? ""
                        : doc.users_id().toString();
                        if (!targetUserId.equals(docUser)) {
                        return false;
                        }
                        LocalDateTime docDate = LocalDateTime.parse(
                        doc.date_creation(), GLPI_DATETIME_FMT
                        );
                        return !docDate.isBefore(before) && !docDate.isAfter(after);
                })
                .map(doc -> {
                        try {
                                byte[] data = glpiServerClient.downloadDocumentById(doc.id());

                                if (!isMimeTypeAllowed(doc.mime())) {
                                        return new TicketInfoDto.MediaFileDto("Error", doc.filename(), doc.mime());
                                }

                                String ext = doc.filename().contains(".")
                                        ? doc.filename().substring(doc.filename().lastIndexOf('.'))
                                        : ".tmp";
                                File tmp = File.createTempFile("glpi_doc_", ext);
                                Files.write(tmp.toPath(), data);

                                String mediaId = apiWhatsappService.uploadMedia(tmp);
                                tmp.delete();

                                return new TicketInfoDto.MediaFileDto(mediaId, doc.filename(), doc.mime());
                        } catch (Exception e) {
                                logger.error("Error procesando documento del GLPI: "+ doc + e.getMessage(), e);
                                return new TicketInfoDto.MediaFileDto("Error", "unknown", "unknown");
                        }
                })
                .collect(Collectors.toList());
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
                HtmlCleaner.cleanHtmlForWhatsApp(glpiTicket.content()),
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
                        // Solucion
                        List<TicketSolution> glpiSolutions = glpiServerClient.getTicketSolutionById(glpiTicket.id());
                        solutionDto = glpiSolutions.stream()
                                .map(solution -> {
                                        String formatted = HtmlCleaner.cleanHtmlForWhatsApp(solution.content());

                                        String solutionStatus = solution.status() == 1L ? "None"
                                                        : solution.status() == 2L ? "En espera"
                                                        : solution.status() == 3L ? "Aceptado"
                                                        : solution.status() == 4L ? "Rechazado"
                                                        : "Indefinido";

                                        List<MediaFileDto> fromLinks = Optional.ofNullable(extractMediaIdsFromLinks(solution.links()))
                                                .orElse(Collections.emptyList());

                                        // Busco Documentos anexados
                                        String solDate   = solution.date_creation();
                                        String solUserId = (String) solution.users_id();

                                        List<MediaFileDto> fromDocs = filterDocuments(Arrays.asList(glpiTicket._documents()), solUserId, solDate);
                                        System.out.println("Docuemnto desde el ticket: " + fromDocs); //! debug

                                        List<MediaFileDto> mediaFiles = new ArrayList<>(fromLinks);
                                        mediaFiles.addAll(fromDocs);
                                        System.out.println("media Files: " + mediaFiles); //! debug

                                        return new TicketSolutionDto(
                                        formatted,
                                        solution.date_creation(),
                                        solutionStatus,
                                        mediaFiles
                                        );
                                })
                                .collect(Collectors.toList());
                }

                // 7) Seguimientos (_notes)
                List<TicketFollowUp> rawNotes = glpiServerClient.TicketWithNotes(glpiTicket.id());
                List<NoteDto> notes = rawNotes.stream()
                        .map(n -> {
                        String content = HtmlCleaner.cleanHtmlForWhatsApp(n.content());
                        List<MediaFileDto> mediaFiles = Optional.ofNullable(extractMediaIdsFromLinks(n.links()))
                                        .orElse(Collections.emptyList());

                        return new NoteDto(
                                n.date_creation(),
                                content,
                                mediaFiles
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
        public Object createTicket(CreateTicket payload, String whatsAppPhone) {

                UserChatEntity user = userChatRepository.findByWhatsappPhone(whatsAppPhone)
                        .orElseThrow(() -> new ServerClientException("Usuario no encontrado para el número de WhatsApp: " + whatsAppPhone));

                CreateTicket ticketToCreate = new CreateTicket(
                        new InputCreateTicket(
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

                responseCreateTicketSuccess response = glpiServerClient.createTicket(ticketToCreate);

                Ticket glpiTicket = glpiServerClient.getTicketById(response.id());

                UserTicketEntity entity = new UserTicketEntity();
                entity.setId(glpiTicket.id());
                entity.setWhatsappPhone(whatsAppPhone);
                entity.setName(glpiTicket.name());
                Long status = glpiTicket.status();
                String statusStr = switch (status.intValue()) {
                                case 1 -> "Nuevo";
                                case 2 -> "En curso (Asignado)";
                                case 3 -> "En curso (Planificado)";
                                case 4 -> "En espera";
                                case 5 -> "Resuelto";
                                case 6 -> "Cerrado";
                                default -> "Indefinido";
                        };
                entity.setStatus(statusStr);
                entity.setUserChat(user);
                userTicketRepository.save(entity);

                return Map.of(
                                                "id", glpiTicket.id(),
                                                "Titulo", glpiTicket.name(),
                                                "correo_de_envio", String.join(",", payload.input()._users_id_requester_notif().alternative_email())
                );
        }

        @Override
        public String getStatusTicket(Long ticketId) {
                Long status = glpiServerClient.getTicketById(ticketId).status();
                return switch (status.intValue()) {
                                case 1 -> "Nuevo";
                                case 2 -> "En curso (Asignado)";
                                case 3 -> "En curso (Planificado)";
                                case 4 -> "En espera";
                                case 5 -> "Resuelto";
                                case 6 -> "Cerrado";
                                default -> "Indefinido";
                        };
        }


        @Override
        public Object refusedOrAcceptedSolutionTicket(SolutionDecisionRequest request, String whatsAppPhone) {
                Boolean _acepted = request.getAccepted();
                Long ticketId = request.getTicketId();
                String contentNote = request.getContent();
                Long status = glpiServerClient.getTicketById(ticketId).status();

                // Verificar que el ticket le pertenece
                Long id = Long.valueOf(ticketId);
                if (!userTicketRepository.existsByWhatsappPhoneAndId(whatsAppPhone, id)) {
                    throw new ServerClientException(
                        "El ticket " + ticketId + " no te pertenece.");
                }
                
                if (status == 5L) {
                        if (_acepted) {
                                RequestUpdateStatus updateStatus = new RequestUpdateStatus(
                                        new InputUpdate(6L, _acepted)
                                );
        
                                glpiServerClient.updateTicketStatusById(ticketId, updateStatus);
                                return Map.of("message", "La solución del ticket ha sido aceptada exitosamente.");
                        } else {
                                // Actualiza el Status del ticket(En progreso)
                                RequestUpdateStatus updateStatus = new RequestUpdateStatus(new InputUpdate(2L));
                                glpiServerClient.updateTicketStatusById(ticketId, updateStatus);

                                // GLPI se encarga de rechazar la solución automáticamente al cambiar ticket-status(5>2)  

                                // Enviar nuevo seguimeinto del ticket
                                CreateNoteForTicket note = new CreateNoteForTicket( new InputFollowup("Ticket", ticketId, contentNote));
                                glpiServerClient.createNoteForTicket(note);

                                return Map.of("message", "La solución del ticket ha sido rechazada exitosamente y se ha notificado el motivo del rechazo.");
                        }          
                } else {
                        return Map.of("message", "El ticket aún no tiene solución.");
                }

        }

        @Override
        public Object createNoteForTicket( Long ticketId, String contentNote, String whatsAppPhone) {

                // Actualiza el Status del ticket(En progreso)
                RequestUpdateStatus updateStatus = new RequestUpdateStatus(new InputUpdate(2L));
                glpiServerClient.updateTicketStatusById(ticketId, updateStatus);

                // Verificar que el ticket le pertenece
                Long id = Long.valueOf(ticketId);
                if (!userTicketRepository.existsByWhatsappPhoneAndId(whatsAppPhone, id)) {
                    throw new ServerClientException(
                        "El ticket " + ticketId + " ya fue cerrado o no tienes acceso al el.");
                }

                CreateNoteForTicket note = new CreateNoteForTicket(new InputFollowup("Ticket", ticketId, contentNote));
                glpiServerClient.createNoteForTicket(note);
                return Map.of("message", "El Seguimiento se envió exitosamente.");
        }
        
}
