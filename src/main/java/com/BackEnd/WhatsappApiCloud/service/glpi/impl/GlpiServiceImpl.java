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
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.BackEnd.WhatsappApiCloud.exception.ServerClientException;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.GlpiDto;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.GlpiDto.*;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.TicketInfoDto;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.TicketInfoDto.*;
import com.BackEnd.WhatsappApiCloud.model.entity.glpi.UserTicketEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.reports.TicketReportEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.user.AttachmentEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatEntity;
import com.BackEnd.WhatsappApiCloud.repository.AttachmentRepository;
import com.BackEnd.WhatsappApiCloud.repository.TicketReportRepository;
import com.BackEnd.WhatsappApiCloud.repository.UserChatRepository;
import com.BackEnd.WhatsappApiCloud.repository.UserTicketRepository;
import com.BackEnd.WhatsappApiCloud.service.glpi.GlpiServerClient;
import com.BackEnd.WhatsappApiCloud.service.glpi.GlpiService;
import com.BackEnd.WhatsappApiCloud.service.glpi.HtmlCleaner;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.WhatsappMediaService;
import com.BackEnd.WhatsappApiCloud.util.enums.AttachmentStatusEnum;

@Service
public class GlpiServiceImpl implements GlpiService {

        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final GlpiServerClient glpiServerClient;

        private final UserChatRepository userChatRepository;
        private final UserTicketRepository userTicketRepository;
        private final AttachmentRepository attachmentRepository;

        private final WhatsappMediaService whatsappMediaService;
        private final TicketReportRepository ticketReportRepository;

        public GlpiServiceImpl(
                        GlpiServerClient glpiServerClient,
                        UserChatRepository userChatRepository,
                        UserTicketRepository userTicketRepository,
                        AttachmentRepository attachmentRepository,
                        WhatsappMediaService whatsappMediaService,
                        TicketReportRepository ticketReportRepository) {
                this.whatsappMediaService = whatsappMediaService;
                this.glpiServerClient = glpiServerClient;
                this.userChatRepository = userChatRepository;
                this.userTicketRepository = userTicketRepository;
                this.attachmentRepository = attachmentRepository;
                this.ticketReportRepository = ticketReportRepository;
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
                                        List<Document_Item> documentItems = glpiServerClient
                                                        .getDocumentItems(link.href());
                                        documentItems.forEach(documentItem -> {
                                                Arrays.stream(documentItem.links())
                                                                .filter(docLink -> "Document".equals(docLink.rel()))
                                                                .forEach(docLink -> {
                                                                        try {
                                                                                byte[] fileData = glpiServerClient
                                                                                                .downloadDocument(
                                                                                                                docLink.href());

                                                                                Tika tika = new Tika();
                                                                                String contentType = tika
                                                                                                .detect(fileData);

                                                                                if (!isMimeTypeAllowed(contentType)) {
                                                                                        mediaFiles.add(new MediaFileDto(
                                                                                                        "Error",
                                                                                                        documentItem.documents_id(),
                                                                                                        contentType));
                                                                                        return;
                                                                                }

                                                                                String extension = getExtensionFromDocumentId(
                                                                                                documentItem.documents_id());

                                                                                File tempFile = File.createTempFile(
                                                                                                "document", extension);
                                                                                Files.write(tempFile.toPath(),
                                                                                                fileData);

                                                                                String mediaId = whatsappMediaService
                                                                                                .uploadMedia(tempFile);
                                                                                mediaFiles.add(new TicketInfoDto.MediaFileDto(
                                                                                                mediaId,
                                                                                                documentItem.documents_id(),
                                                                                                contentType));

                                                                                tempFile.delete();
                                                                        } catch (Exception e) {
                                                                                logger.error("Error procesando documento del GLPI: "
                                                                                                + documentItem
                                                                                                + e.getMessage(), e);
                                                                                mediaFiles.add(new TicketInfoDto.MediaFileDto(
                                                                                                "Error", "unknown",
                                                                                                "unknown"));
                                                                        }
                                                                });
                                        });
                                });
                return mediaFiles;
        }


        // ========= Procesa un documento GLPI y lo sube a WhatsApp =========
        private MediaFileDto processDocument(DocumentGlpi doc) {

                try {
                        byte[] data = glpiServerClient.downloadDocumentById(doc.id());

                        if (!isMimeTypeAllowed(doc.mime())) {
                                return new MediaFileDto("Error", doc.filename(), doc.mime());
                        }

                        String ext = doc.filename().contains(".")
                                        ? doc.filename().substring(doc.filename().lastIndexOf('.'))
                                        : ".tmp";

                        File tmp = File.createTempFile("glpi_doc_", ext);
                        Files.write(tmp.toPath(), data);

                        String mediaId = whatsappMediaService.uploadMedia(tmp);
                        tmp.delete();

                        return new MediaFileDto(mediaId, doc.filename(), doc.mime());

                } catch (Exception e) {
                        logger.error("Error procesando documento GLPI: " + doc + " | " + e.getMessage(), e);
                        return new MediaFileDto("Error", doc.filename(), doc.mime());
                }
        }

        // ========= Filtra los documentos de un ticket (Soluciones)=========
        private static final DateTimeFormatter GLPI_DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        public List<MediaFileDto> filterSolutionDocuments(
                        List<DocumentGlpi> docs,
                        String targetUserId,
                        String referenceDateStr) {

                LocalDateTime ref = LocalDateTime.parse(referenceDateStr, GLPI_DATETIME_FMT);
                LocalDateTime before = ref.minusMinutes(10);
                LocalDateTime after = ref.plusMinutes(10);

                return docs.stream()
                                .filter(doc -> {

                                        // Filtrar usuario exacto
                                        String docUser = doc.users_id() == null ? "" : doc.users_id().toString();
                                        if (!targetUserId.equals(docUser))
                                                return false;

                                        // Filtrar rango de tiempo
                                        LocalDateTime docDate = LocalDateTime.parse(doc.date_creation(),
                                                        GLPI_DATETIME_FMT);
                                        return !docDate.isBefore(before) && !docDate.isAfter(after);
                                })
                                .map(this::processDocument)
                                .collect(Collectors.toList());
        }

        // ========= Filtra los documentos de un ticket (Seguimientos)=========
        public List<MediaFileDto> filterNoteDocuments(
                        List<DocumentGlpi> docs,
                        List<String> technicianIds) {

                return docs.stream()
                                .filter(doc -> {
                                        String docUser = doc.users_id() == null ? "" : doc.users_id().toString();
                                        boolean userMatch = technicianIds.stream()
                                                        .anyMatch(techId -> techId.toString().equals(docUser));
                                        return userMatch;
                                })
                                .map(this::processDocument)
                                .collect(Collectors.toList());
        }


        // ================== Obtener ticket =================
        @Override
        public TicketInfoDto getInfoTicketById(Long ticketId) {
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
                Optional<GlpiDto.UserTicket> maybeRequester = userTickets.stream()
                                .filter(t -> t.type() == 1)
                                .findFirst();
                String requester = "";
                if (maybeRequester.isPresent()) {
                        GlpiDto.UserTicket ticket = maybeRequester.get();
                        Long userId = ticket.users_id();

                        if (userId == 0L) {
                                String altEmail = ticket.alternative_email();
                                requester = altEmail;
                        } else {
                                List<Usermail> mailList = glpiServerClient.getEmailUser(userId);
                                Optional<Usermail> maybeMail = mailList.stream().findFirst();
                                requester = maybeMail
                                                .map(Usermail::email)
                                                .orElse("unknown");
                        }
                }
                // No se encontro solicitante type = 1
                else {
                        requester = "—sin solicitante—";
                }

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
                                                                                : glpiTicket.status() == 4L
                                                                                                ? "En espera"
                                                                                                : glpiTicket.status() == 5L
                                                                                                                ? "Resuelto"
                                                                                                                : glpiTicket.status() == 6L
                                                                                                                                ? "Cerrado"
                                                                                                                                : "Indefinido",
                                HtmlCleaner.cleanHtmlForWhatsApp(glpiTicket.content()),
                                glpiTicket.urgency() == 1L ? "Muy baja"
                                                : glpiTicket.urgency() == 2L ? "Baja"
                                                                : glpiTicket.urgency() == 3L ? "Media"
                                                                                : glpiTicket.urgency() == 4L ? "Alta"
                                                                                                : glpiTicket.urgency() == 5L
                                                                                                                ? "Muy Alta"
                                                                                                                : glpiTicket.urgency() == 6L
                                                                                                                                ? "Primordial"
                                                                                                                                : "Indefinido",
                                glpiTicket.impact() == 1L ? "Muy baja"
                                                : glpiTicket.impact() == 2L ? "Baja"
                                                                : glpiTicket.impact() == 3L ? "Media"
                                                                                : glpiTicket.impact() == 4L ? "Alta"
                                                                                                : glpiTicket.impact() == 5L
                                                                                                                ? "Muy Alta"
                                                                                                                : glpiTicket.impact() == 6L
                                                                                                                                ? "Primordial"
                                                                                                                                : "Indefinido",
                                glpiTicket.priority() == 1L ? "Muy baja"
                                                : glpiTicket.priority() == 2L ? "Baja"
                                                                : glpiTicket.priority() == 3L ? "Media"
                                                                                : glpiTicket.priority() == 4L ? "Alta"
                                                                                                : glpiTicket.priority() == 5L
                                                                                                                ? "Muy Alta"
                                                                                                                : glpiTicket.priority() == 6L
                                                                                                                                ? "Primordial"
                                                                                                                                : "Indefinido",
                                glpiTicket.itilcategories_id(),
                                glpiTicket.type() == 1L ? "Incidencia"
                                                : glpiTicket.type() == 2L ? "Solicitud" : "Indefinido",
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
                                                                                                : solution.status() == 4L
                                                                                                                ? "Rechazado"
                                                                                                                : "Indefinido";

                                                List<MediaFileDto> fromLinks = Optional
                                                                .ofNullable(extractMediaIdsFromLinks(solution.links()))
                                                                .orElse(Collections.emptyList());

                                                // Busco Documentos anexados
                                                List<MediaFileDto> fromDocs = filterSolutionDocuments(
                                                                Arrays.asList(glpiTicket._documents()),
                                                                solution.users_id().toString(),
                                                                solution.date_creation());

                                                List<MediaFileDto> mediaFiles = new ArrayList<>(fromLinks);
                                                mediaFiles.addAll(fromDocs);

                                                return new TicketSolutionDto(
                                                                formatted,
                                                                solution.date_creation(),
                                                                solutionStatus,
                                                                mediaFiles);
                                        })
                                        .collect(Collectors.toList());
                }

                // 7) Seguimientos (_notes)
                List<TicketFollowUp> rawNotes = glpiServerClient.TicketWithNotes(glpiTicket.id());
                List<NoteDto> notes = rawNotes.stream()
                                .map(n -> {
                                        String content = HtmlCleaner.cleanHtmlForWhatsApp(n.content());
                                        List<MediaFileDto> fromLinks = Optional
                                                        .ofNullable(extractMediaIdsFromLinks(n.links()))
                                                        .orElse(Collections.emptyList());

                                        // Busco Documentos anexados
                                        List<String> technicianIds = techDtos.stream()
                                                        .map(TechDto::name)
                                                        .collect(Collectors.toList());

                                        List<MediaFileDto> fromDocs = filterNoteDocuments(
                                                        Arrays.asList(glpiTicket._documents()),
                                                        technicianIds);

                                        List<MediaFileDto> mediaFiles = new ArrayList<>(fromLinks);
                                        mediaFiles.addAll(fromDocs);

                                        return new NoteDto(
                                                        n.date_creation(),
                                                        content,
                                                        mediaFiles);
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
        public void attachRecentWhatsappMediaToTicket(String waId, long ticketId, int minutesWindow) {
                // Validaciones de estado/propiedad
                String st = getStatusTicket(ticketId);
                if ("Cerrado".equals(st))
                        throw new ServerClientException(
                                        "El ticket " + ticketId + " está cerrado, no se pueden adjuntar más archivos.");
                if (!userTicketRepository.existsByWhatsappPhoneAndId(waId, ticketId))
                        throw new ServerClientException("El ticket " + ticketId + " no te pertenece.");

                // Ventana de sesión (si existe)
                UserChatEntity user = userChatRepository.findByWhatsappPhone(waId)
                                .orElseThrow(() -> new ServerClientException("Usuario no encontrado: " + waId));

                Instant now = Instant.now();
                Instant sessionStart = user.getAttachStartedAt();
                Integer ttlMin = user.getAttachTtlMinutes();

                // start: si hay sesión, usa attachStartedAt; si no, usa now - minutesWindow
                Instant start = (sessionStart != null) ? sessionStart : now.minus(Duration.ofMinutes(minutesWindow));

                // end: si hay TTL, limita a (attachStartedAt + TTL); si no, usa now
                Instant end = (sessionStart != null && ttlMin != null)
                                ? sessionStart.plus(Duration.ofMinutes(ttlMin))
                                : now;
                if (end.isAfter(now))
                        end = now; // cap al “ahora”

                // Selección en BD usando BETWEEN (evita mezclar sesiones)
                List<AttachmentEntity> list = attachmentRepository
                                .findByWhatsappPhoneAndAttachmentStatusAndTimestampBetween(
                                                waId, AttachmentStatusEnum.UNUSED, start, end);

                for (AttachmentEntity att : list) {
                        File tmp = null;
                        try {
                                // 1) Descargar desde WhatsApp a archivo temporal, (nul = default name)
                                tmp = whatsappMediaService.downloadMediaToTemp(att.getAttachmentID(), null);

                                // 2) Subir a GLPI como Document
                                var up = glpiServerClient.uploadDocument(tmp, tmp.getName(), 0);
                                long docId = up.id();

                                // 2.1) Si es una imagen con caption, crear seguimiento con el texto de caption
                                if (att.getCaption() != null && !att.getCaption().isBlank()) {
                                        // Actualiza el Status del ticket(En progreso)
                                        RequestUpdateStatus updateStatus = new RequestUpdateStatus(new InputUpdate(2L));
                                        glpiServerClient.updateTicketStatusById(ticketId, updateStatus);
                                        CreateNoteForTicket note = new CreateNoteForTicket(
                                                        new InputFollowup("Ticket", ticketId, att.getCaption()));
                                        glpiServerClient.createNoteForTicket(note);
                                }

                                // 3) Enlazar Document al Ticket
                                glpiServerClient.linkDocumentToTicket(docId, ticketId);

                                // 4) Marcar como usado y guardar trazas
                                att.setAttachmentStatus(AttachmentStatusEnum.USED);
                                att.setTicketId(ticketId);
                                att.setGpliDocuemntId(docId);
                                attachmentRepository.save(att);

                        } catch (Exception ex) {
                                att.setAttachmentStatus(AttachmentStatusEnum.INVALID);
                                attachmentRepository.save(att);
                                String msg = "Error al adjuntar el archivo " + att.getAttachmentID()
                                                + " al ticket " + ticketId + ": " + ex.getMessage();
                                logger.error(msg, ex);
                                throw new ServerClientException(ex.getMessage(), ex);
                        } finally {
                                if (tmp != null && tmp.exists()) {
                                        try {
                                                Files.deleteIfExists(tmp.toPath());
                                                Files.deleteIfExists(tmp.getParentFile().toPath());
                                        } catch (IOException ignore) {
                                                logger.warn("No se pudo borrar el archivo temporal: {}",
                                                                tmp.getAbsolutePath());
                                        }
                                }
                        }
                }
        }

        @Override
        @Transactional
        public Object createTicket(CreateTicket payload, String whatsAppPhone) {

                UserChatEntity user = userChatRepository.findByWhatsappPhone(whatsAppPhone)
                                .orElseThrow(() -> new ServerClientException(
                                                "Usuario no encontrado para el número de WhatsApp: " + whatsAppPhone));

                // --- Requester notif
                UserIdRequesterNotif safeReqNotif = new UserIdRequesterNotif(
                                payload.input()._users_id_requester_notif().use_notification(),
                                payload.input()._users_id_requester_notif().alternative_email());

                // --- Observers (opcionales)
                var obsIds = payload.input()._users_id_observer(); // puede ser null
                var obsNotifIn = payload.input()._users_id_observer_notif(); // puede ser null

                // Si no hay observadores o el bloque notif viene null, no lo mandes
                List<Long> safeObsIds = (obsIds == null || obsIds.isEmpty()) ? null : obsIds;
                UserIdObserverNotif safeObsNotif = null;

                if (safeObsIds != null && obsNotifIn != null) {
                        if (obsNotifIn.use_notification() != null
                                        && obsNotifIn.alternative_email() != null
                                        && obsNotifIn.use_notification().size() == obsNotifIn.alternative_email().size()
                                        && obsNotifIn.use_notification().size() == safeObsIds.size()) {

                                safeObsNotif = new UserIdObserverNotif(
                                                obsNotifIn.use_notification(),
                                                obsNotifIn.alternative_email());
                        } else {
                                throw new ServerClientException(
                                                "Los arrays de observadores no coinciden en longitud (ids/use_notification/emails).");
                        }
                }

                CreateTicket ticketToCreate = new CreateTicket(
                                new InputCreateTicket(
                                                payload.input().name(),
                                                payload.input().content(),
                                                payload.input().entities_id(),
                                                payload.input().requesttypes_id(),
                                                payload.input()._users_id_requester(),
                                                safeReqNotif,
                                                safeObsIds,
                                                safeObsNotif,
                                                payload.input().users_id_lastupdater()));

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

                // Reporteria de ticket creados
                TicketReportEntity ticketReport = new TicketReportEntity();
                ticketReport.setTicketId(glpiTicket.id());
                ticketReport.setUserRequester(whatsAppPhone);
                ticketReport.setNameTicket(glpiTicket.name());
                ticketReportRepository.save(ticketReport);

                // Adjuntar archivos recientes de WhatsApp al ticket
                attachRecentWhatsappMediaToTicket(whatsAppPhone, glpiTicket.id(), 10);

                return Map.of(
                                "id", glpiTicket.id(),
                                "Titulo", glpiTicket.name(),
                                "correo_de_envio",
                                String.join(",", payload.input()._users_id_requester_notif().alternative_email()));
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

}
