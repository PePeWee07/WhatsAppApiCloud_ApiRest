package com.BackEnd.WhatsappApiCloud.service.userChat.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.BackEnd.WhatsappApiCloud.exception.UserNotFoundException;
import com.BackEnd.WhatsappApiCloud.model.dto.erp.ErpUserDto;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.TicketInfoDto;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.TicketInfoDto.MediaFileDto;
import com.BackEnd.WhatsappApiCloud.model.dto.user.UserChatSessionDto;
import com.BackEnd.WhatsappApiCloud.model.dto.user.UserChatFullDto;
import com.BackEnd.WhatsappApiCloud.model.dto.user.UserTicketDto;
import com.BackEnd.WhatsappApiCloud.model.entity.glpi.UserTicketEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.BackEnd.WhatsappApiCloud.repository.UserChatRepository;
import com.BackEnd.WhatsappApiCloud.repository.UserTicketRepository;
import com.BackEnd.WhatsappApiCloud.service.erp.ErpServerClient;
import com.BackEnd.WhatsappApiCloud.service.glpi.GlpiService;
import com.BackEnd.WhatsappApiCloud.service.userChat.UserchatService;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.ApiWhatsappService;
import com.fasterxml.jackson.core.JsonProcessingException;

@Service
public class UserChatServiceImpl implements UserchatService {

    private final UserChatRepository repo;
    private final UserTicketRepository  userTicketRepository;
    private final ErpServerClient erpClient;
    private final GlpiService glpiService;
    private final ApiWhatsappService apiWhatsappService;


    public UserChatServiceImpl(UserChatRepository repo, ErpServerClient erpClient, UserTicketRepository  userTicketRepository, GlpiService glpiService, ApiWhatsappService apiWhatsappService) {
        this.erpClient = erpClient;
        this.repo = repo;
        this.userTicketRepository = userTicketRepository;
        this.glpiService = glpiService;
        this.apiWhatsappService = apiWhatsappService;
    }

    private UserTicketDto mapToUserTicketDto(UserTicketEntity ticket) {
        String status = glpiService.getStatusTicket(ticket.getId());

        if ("Cerrado".equals(status)) {
            userTicketRepository.delete(ticket);
            return null;
        } else {
            ticket.setStatus(status);
            userTicketRepository.save(ticket);
        }

        return new UserTicketDto(ticket.getId(), ticket.getName(), status);
    }


    public List<UserTicketDto> listOpenTickets(String whatsAppPhone) {
        List<UserTicketEntity> tickets = userTicketRepository.findByWhatsappPhone(whatsAppPhone);
        //! List<UserTicketEntity> tickets = user.getTickets(); // ✅ usamos la colección mapeada por Hibernate

        return tickets.stream()
            .map(ticket -> mapToUserTicketDto(ticket))
            .filter(dto ->  dto != null)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserChatFullDto findByIdentificacion(String identificacion) {
        UserChatEntity user = repo.findByIdentificacion(identificacion)
            .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario con identificacion: " + identificacion));
            
            List<UserChatSessionDto> sesionesDto = user.getChatSessions().stream()
            .map(cs -> new UserChatSessionDto(
                cs.getId(),
                cs.getWhatsappPhone(),
                cs.getMessageCount(),
                cs.getStartTime(),
                cs.getEndTime()))
            .collect(Collectors.toList());
        
        List<UserTicketDto> ticketsDto = listOpenTickets(user.getWhatsappPhone());

        UserChatFullDto fullDto = new UserChatFullDto();
        fullDto.setId(user.getId());
        fullDto.setIdentificacion(user.getIdentificacion());
        fullDto.setWhatsappPhone(user.getWhatsappPhone());
        fullDto.setPreviousResponseId(user.getPreviousResponseId());
        fullDto.setLimitQuestions(user.getLimitQuestions());
        fullDto.setFirstInteraction(user.getFirstInteraction());
        fullDto.setLastInteraction(user.getLastInteraction());
        fullDto.setNextResetDate(user.getNextResetDate());
        fullDto.setConversationState(user.getConversationState().name());
        fullDto.setLimitStrike(user.getLimitStrike());
        fullDto.setBlock(user.isBlock());
        fullDto.setBlockingReason(user.getBlockingReason());
        fullDto.setValidQuestionCount(user.getValidQuestionCount());
        fullDto.setChatSessions(sesionesDto);
        fullDto.setUserTickets(ticketsDto);
        
        ErpUserDto erpUser = erpClient.getUser(identificacion);
        fullDto.setErpUser(erpUser);

        return fullDto;
    }


    @Override
    @Transactional(readOnly = true)
    public UserChatFullDto findByWhatsappPhone(String whatsAppPhone) {
        UserChatEntity user = repo.findByWhatsappPhone(whatsAppPhone)
            .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario con whatsAppPhone: " + whatsAppPhone));
        
        List<UserChatSessionDto> sesionesDto = user.getChatSessions().stream()
        .map(cs -> new UserChatSessionDto(
                cs.getId(),
                cs.getWhatsappPhone(),
                cs.getMessageCount(),
                cs.getStartTime(),
                cs.getEndTime()))
            .collect(Collectors.toList());
        List<UserTicketDto> ticketsDto = listOpenTickets(user.getWhatsappPhone());

        UserChatFullDto fullDto = new UserChatFullDto();
        fullDto.setId(user.getId());
        fullDto.setIdentificacion(user.getIdentificacion());
        fullDto.setWhatsappPhone(user.getWhatsappPhone());
        fullDto.setPreviousResponseId(user.getPreviousResponseId());
        fullDto.setLimitQuestions(user.getLimitQuestions());
        fullDto.setFirstInteraction(user.getFirstInteraction());
        fullDto.setLastInteraction(user.getLastInteraction());
        fullDto.setNextResetDate(user.getNextResetDate());
        fullDto.setConversationState(user.getConversationState().name());
        fullDto.setLimitStrike(user.getLimitStrike());
        fullDto.setBlock(user.isBlock());
        fullDto.setBlockingReason(user.getBlockingReason());
        fullDto.setValidQuestionCount(user.getValidQuestionCount());
        fullDto.setChatSessions(sesionesDto);
        fullDto.setUserTickets(ticketsDto);

        if ("Anonymus".equals(user.getIdentificacion())) {
            fullDto.setErpUser(null);
        } else {
            ErpUserDto erpUser = erpClient.getUser(user.getIdentificacion());
            fullDto.setErpUser(erpUser);
        }

        return fullDto;
    }

    // ================ Paginar todos los usuarios ======================
    @Override
    @Transactional(readOnly = true)
    public Page<UserChatFullDto> usersTable(int page, int size, String sortBy, String direction) {

        Sort sort = Sort.by(sortBy);
        sort = "desc".equalsIgnoreCase(direction) ? sort.descending() : sort.ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserChatEntity> pageLocal = repo.findAll(pageable);

        List<UserChatFullDto> dtos = pageLocal.getContent().stream()
            .map(user -> { 
                List<UserChatSessionDto> sesionesDto = user.getChatSessions().stream()
                    .map(cs -> new UserChatSessionDto(
                        cs.getId(),
                        cs.getWhatsappPhone(),
                        cs.getMessageCount(),
                        cs.getStartTime(),
                        cs.getEndTime()))
                        .collect(Collectors.toList());
                
                List<UserTicketDto> ticketsDto = listOpenTickets(user.getWhatsappPhone());

                UserChatFullDto fullDto = new UserChatFullDto();
                fullDto.setId(user.getId());
                fullDto.setIdentificacion(user.getIdentificacion());
                fullDto.setWhatsappPhone(user.getWhatsappPhone());
                fullDto.setPreviousResponseId(user.getPreviousResponseId());
                fullDto.setLimitQuestions(user.getLimitQuestions());
                fullDto.setFirstInteraction(user.getFirstInteraction());
                fullDto.setLastInteraction(user.getLastInteraction());
                fullDto.setNextResetDate(user.getNextResetDate());
                fullDto.setConversationState(user.getConversationState().name());
                fullDto.setLimitStrike(user.getLimitStrike());
                fullDto.setBlock(user.isBlock());
                fullDto.setBlockingReason(user.getBlockingReason());
                fullDto.setValidQuestionCount(user.getValidQuestionCount());
                fullDto.setChatSessions(sesionesDto);
                fullDto.setUserTickets(ticketsDto);
                
                if ("Anonymus".equals(user.getIdentificacion())) {
                    fullDto.setErpUser(null);
                } else {
                    ErpUserDto erpUser = erpClient.getUser(user.getIdentificacion());
                    fullDto.setErpUser(erpUser);
                }
                

                return fullDto;
            })
            .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, pageLocal.getTotalElements());
    }

    //! (Eliminar) ============  Paginar usuarios por ultima interaccion  ============
    @Override
    @Transactional(readOnly = true)
    public Page<UserChatFullDto> tablefindByLastInteraction(int page, int size, String sortBy, String direction, LocalDateTime inicio, LocalDateTime fin) {

        Sort sort = Sort.by(sortBy);
        sort = "desc".equalsIgnoreCase(direction) ? sort.descending() : sort.ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserChatEntity> pageLocal = repo.findByPreviousResponseIdIsNotNullAndLastInteractionBetween(inicio, fin, pageable);

        List<UserChatFullDto> dtos = pageLocal.getContent().stream()
            .map(user -> {
                List<UserChatSessionDto> sesionesDto = user.getChatSessions().stream()
                    .map(cs -> new UserChatSessionDto(
                        cs.getId(),
                        cs.getWhatsappPhone(),
                        cs.getMessageCount(),
                        cs.getStartTime(),
                        cs.getEndTime()))
                    .collect(Collectors.toList());

                List<UserTicketDto> ticketsDto = listOpenTickets(user.getWhatsappPhone());

                UserChatFullDto fullDto = new UserChatFullDto();
                fullDto.setId(user.getId());
                fullDto.setIdentificacion(user.getIdentificacion());
                fullDto.setWhatsappPhone(user.getWhatsappPhone());
                fullDto.setPreviousResponseId(user.getPreviousResponseId());
                fullDto.setLimitQuestions(user.getLimitQuestions());
                fullDto.setFirstInteraction(user.getFirstInteraction());
                fullDto.setLastInteraction(user.getLastInteraction());
                fullDto.setNextResetDate(user.getNextResetDate());
                fullDto.setConversationState(user.getConversationState().name());
                fullDto.setLimitStrike(user.getLimitStrike());
                fullDto.setBlock(user.isBlock());
                fullDto.setBlockingReason(user.getBlockingReason());
                fullDto.setValidQuestionCount(user.getValidQuestionCount());
                fullDto.setChatSessions(sesionesDto);
                fullDto.setUserTickets(ticketsDto);
                
                if ("Anonymus".equals(user.getIdentificacion())) {
                    fullDto.setErpUser(null);
                } else {
                    ErpUserDto erpUser = erpClient.getUser(user.getIdentificacion());
                    fullDto.setErpUser(erpUser);
                }

                return fullDto;
            })
            .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, pageLocal.getTotalElements());
    }

    // ============ Paginar usuarios por chatSession ===========
    @Override
    @Transactional(readOnly = true)
    public Page<UserChatFullDto> tablefindByChatSessionStart(int page, int size, String sortBy, String direction, LocalDateTime inicio, LocalDateTime fin) {

        Sort sort = Sort.by(sortBy);
        sort = "desc".equalsIgnoreCase(direction) ? sort.descending() : sort.ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserChatEntity> pageLocal = repo.findByChatSessionsOverlapping(inicio, fin, pageable);

        List<UserChatFullDto> dtos = pageLocal.getContent().stream()
            .map(user -> {
                List<UserChatSessionDto> sesionesDto = user.getChatSessions().stream()
                    .map(cs -> new UserChatSessionDto(
                        cs.getId(),
                        cs.getWhatsappPhone(),
                        cs.getMessageCount(),
                        cs.getStartTime(),
                        cs.getEndTime()))
                    .collect(Collectors.toList());
                    
                List<UserTicketDto> ticketsDto = listOpenTickets(user.getWhatsappPhone());

                UserChatFullDto fullDto = new UserChatFullDto();
                fullDto.setId(user.getId());
                fullDto.setIdentificacion(user.getIdentificacion());
                fullDto.setWhatsappPhone(user.getWhatsappPhone());
                fullDto.setPreviousResponseId(user.getPreviousResponseId());
                fullDto.setLimitQuestions(user.getLimitQuestions());
                fullDto.setFirstInteraction(user.getFirstInteraction());
                fullDto.setLastInteraction(user.getLastInteraction());
                fullDto.setNextResetDate(user.getNextResetDate());
                fullDto.setConversationState(user.getConversationState().name());
                fullDto.setLimitStrike(user.getLimitStrike());
                fullDto.setBlock(user.isBlock());
                fullDto.setBlockingReason(user.getBlockingReason());
                fullDto.setValidQuestionCount(user.getValidQuestionCount());
                fullDto.setChatSessions(sesionesDto);
                fullDto.setUserTickets(ticketsDto);
                
                if ("Anonymus".equals(user.getIdentificacion())) {
                    fullDto.setErpUser(null);
                } else {
                    ErpUserDto erpUser = erpClient.getUser(user.getIdentificacion());
                    fullDto.setErpUser(erpUser);
                }

                return fullDto;
            })
            .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, pageLocal.getTotalElements());
    }

    // ============ Actualizar Usuario ============
    @Override
    @Transactional
    public UserChatFullDto userUpdate(Long id, Map<String, Object> updates) {
        UserChatEntity user = repo.findById(id)
            .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con id " + id));

        if (updates.containsKey("previousResponseId")) {
            Object threadVal = updates.get("previousResponseId");
            if (threadVal instanceof String) {
                user.setPreviousResponseId((String) threadVal);
            }
        }
        if (updates.containsKey("limitQuestions")) {
            Object limQ = updates.get("limitQuestions");
            if (limQ instanceof Number) {
                user.setLimitQuestions(((Number) limQ).intValue());
            } else if (limQ instanceof String) {
                int parsed = Integer.parseInt((String) limQ);
                user.setLimitQuestions(parsed);
            }
        }
        if (updates.containsKey("limitStrike")) {
            Object limS = updates.get("limitStrike");
            if (limS instanceof Number) {
                user.setLimitStrike(((Number) limS).intValue());
            }
        }
        if (updates.containsKey("block")) {
            Object blk = updates.get("block");
            if (blk instanceof Boolean) {
                user.setBlock((Boolean) blk);
            } else if (blk instanceof String) {
                user.setBlock(Boolean.parseBoolean((String) blk));
            }
        }
        if (updates.containsKey("blockingReason")) {
            Object reason = updates.get("blockingReason");
            if (reason instanceof String) {
                user.setBlockingReason((String) reason);
            }
        }

        UserChatEntity userEntity = repo.save(user);
        userEntity.getChatSessions().size();

        List<UserChatSessionDto> sesionesDto = userEntity.getChatSessions().stream()
            .map(cs -> new UserChatSessionDto(
                cs.getId(),
                cs.getWhatsappPhone(),
                cs.getMessageCount(),
                cs.getStartTime(),
                cs.getEndTime()))
            .collect(Collectors.toList());

        List<UserTicketDto> ticketsDto = listOpenTickets(userEntity.getWhatsappPhone());

        UserChatFullDto fullDto = new UserChatFullDto();
        fullDto.setId(userEntity.getId());
        fullDto.setWhatsappPhone(userEntity.getWhatsappPhone());
        fullDto.setPreviousResponseId(userEntity.getPreviousResponseId());
        fullDto.setLimitQuestions(userEntity.getLimitQuestions());
        fullDto.setFirstInteraction(userEntity.getFirstInteraction());
        fullDto.setLastInteraction(userEntity.getLastInteraction());
        fullDto.setNextResetDate(userEntity.getNextResetDate());
        fullDto.setConversationState(userEntity.getConversationState().name());
        fullDto.setLimitStrike(userEntity.getLimitStrike());
        fullDto.setBlock(userEntity.isBlock());
        fullDto.setBlockingReason(userEntity.getBlockingReason());
        fullDto.setValidQuestionCount(userEntity.getValidQuestionCount());
        fullDto.setChatSessions(sesionesDto);
        fullDto.setUserTickets(ticketsDto);

        if ("Anonymus".equals(userEntity.getIdentificacion())) {
            fullDto.setErpUser(null); // Asignar null para usuarios "Anonymus"
        } else {
            ErpUserDto erpUser = erpClient.getUser(userEntity.getIdentificacion());
            fullDto.setErpUser(erpUser);
        }

        return fullDto;
    }

    // ============ Fragmentar mensajes largos ============
    private List<String> splitMessage(String message, int maxLength) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < message.length()) {
            int end = Math.min(start + maxLength, message.length());
            parts.add(message.substring(start, end));
            start = end;
        }
        return parts;
    }

    // ============ Usuario solicita info del Ticket ============
    @Override
    public TicketInfoDto userRequestTicketInfo(String whatsAppPhone, String ticketId) throws IOException {

        // 1) Verificar que el usuario existe
        repo.findByWhatsappPhone(whatsAppPhone)
            .orElseThrow(() -> new UserNotFoundException(
                "Usuario no encontrado para el número: " + whatsAppPhone));

        // 2) Verificar que el ticket le pertenece
        // Long id = Long.valueOf(ticketId);
        // if (!userTicketRepository.existsByWhatsappPhoneAndId(whatsAppPhone, id)) {
        //     throw new ServerClientException(
        //         "El ticket " + ticketId + " no pertenece al usuario " + whatsAppPhone);
        // }

        // 3) Recuperar la info limpia del ticket
        TicketInfoDto info = glpiService.getInfoTicketById(ticketId);

        // 4) Construir un mensaje resumen
        StringBuilder sb = new StringBuilder();
        sb.append(" > *Información del Ticket*\n");
        sb.append("`ID:` ").append(info.ticket().id()).append("\n");
        sb.append("`Titulo:` ").append(info.ticket().name()).append("\n");
        sb.append("`Tipo:` ").append(info.ticket().type()).append("\n");
        sb.append("`Estado:` ").append(info.ticket().status()).append("\n");
        sb.append("`Fecha de apertura:` ").append(info.ticket().date_creation()).append("\n");
        if (info.ticket().closedate() != null) {
            sb.append("`Fecha de cierre:` ").append(info.ticket().closedate()).append("\n");
        }

        // 4.1) Información de técnicos asignados
        if (!info.assigned_techs().isEmpty()) {
            sb.append("\n*Técnicos Asignados:*\n");
            for (TicketInfoDto.TechDto tech : info.assigned_techs()) {
                sb.append("- ").append(tech.firstname()).append(" ").append(tech.realname()).append("\n");
            }
        } else {
            sb.append("\n").append("_Tu Ticket aun no está asignado a un técnico_");
        }

        // 4.2) Información de la solución (si existe y no está rechazada)
        boolean hasValidSolution = false;
        if (!info.solutions().isEmpty()) {
            TicketInfoDto.TicketSolutionDto solution = info.solutions().stream()
                .filter(s -> !"Rechazado".equalsIgnoreCase(s.status()))
                .findFirst()
                .orElse(null);

            if (solution != null) {
                hasValidSolution = true;
                sb.append("\n").append("> *Solución:*\n");
                sb.append("`Fecha de resolución:` ").append(solution.date_creation()).append("\n\n");
                sb.append(solution.content()).append("\n");

                // Enviar imágenes asociadas a la solución
                for (MediaFileDto media : solution.mediaFiles()) {
                    if ("Error".equals(media.mediaId())) {
                        sb.append("\n⚠️ _El archivo '").append(media.name()).append("' no se pudo enviar porque su formato no es compatible._\n");
                        continue;
                    }

                    if (media.mimeType().startsWith("image/")) {
                        apiWhatsappService.sendImageMessageById(whatsAppPhone, media.mediaId(), "📎Adjunto de la solución del ticket");
                    } else if (
                        media.mimeType().startsWith("application/") ||
                        "text/plain".equals(media.mimeType()) ||
                        "text/csv".equals(media.mimeType())
                    ) {
                        apiWhatsappService.sendDocumentMessageById(whatsAppPhone, media.mediaId(), "📎Adjunto de la solución del ticket", media.name());
                    }

                    apiWhatsappService.deleteMediaById(media.mediaId());
                }
            }
        }

        // 4.3) Último seguimiento (si existe)
        if (!hasValidSolution && info.notes() != null && !info.notes().isEmpty()) {
            TicketInfoDto.NoteDto lastNote = info.notes().get(info.notes().size() - 1);
            sb.append("\n").append("> *Último Seguimiento:*\n");
            sb.append("`Fecha:` ").append(lastNote.date_creation()).append("\n\n");
            sb.append(lastNote.content()).append("\n");

            // Enviar imágenes asociadas al seguimiento
            for (MediaFileDto media : lastNote.mediaFiles()) {
                if ("Error".equals(media.mediaId())) {
                    sb.append("\n⚠️ _El archivo '").append(media.name()).append("' no se pudo enviar porque su formato no es compatible._\n");
                    continue;
                }

                if (media.mimeType().startsWith("image/")) {
                    apiWhatsappService.sendImageMessageById(whatsAppPhone, media.mediaId(), "📎Adjunto al seguimiento del ticket");
                } else if (
                        media.mimeType().startsWith("application/") ||
                        "text/plain".equals(media.mimeType()) ||
                        "text/csv".equals(media.mimeType())
                    ) {
                    apiWhatsappService.sendDocumentMessageById(whatsAppPhone, media.mediaId(), "📎Adjunto al seguimiento del ticket", media.name());
                } 

                apiWhatsappService.deleteMediaById(media.mediaId());
            }
        }

        // 5) Dividir el mensaje si excede el límite de 4096 caracteres
        String message = sb.toString();
        if (message.length() > 4096) {
            List<String> parts = splitMessage(message, 4096);
            for (String part : parts) {
                apiWhatsappService.sendMessage(new MessageBody(whatsAppPhone, part));
            }
        } else {
            apiWhatsappService.sendMessage(new MessageBody(whatsAppPhone, message));
        }

        return info;
    }
   
    // ============ Usuario solicita sus Tickets abiertos ============
    @Override
    @Transactional
    public List<UserTicketDto> userRequestTicketList(String whatsAppPhone) throws JsonProcessingException {
        UserChatEntity user = repo.findByWhatsappPhone(whatsAppPhone)
            .orElseThrow(() -> new UserNotFoundException("No se encontró el usuario con el teléfono: " + whatsAppPhone));

        List<UserTicketDto> ticketsDto = listOpenTickets(user.getWhatsappPhone());

        StringBuilder sb = new StringBuilder();
        sb.append("> *Lista de Tickets Abiertos:*\n\n");

        if (ticketsDto.isEmpty()) {
            sb.append("_No tienes tickets abiertos en este momento._");
        } else {
            for (UserTicketDto ticket : ticketsDto) {
                sb.append("`ID:` ").append(ticket.getId()).append("\n");
                sb.append("`Título:` ").append(ticket.getName()).append("\n");
                sb.append("`Estado:` ").append(ticket.getStatus()).append("\n\n");
            }
        }

        String message = sb.toString();
        if (message.length() > 4096) {
            List<String> parts = splitMessage(message, 4096);
            for (String part : parts) {
                apiWhatsappService.sendMessage(new MessageBody(whatsAppPhone, part));
            }
        } else {
            apiWhatsappService.sendMessage(new MessageBody(whatsAppPhone, message));
        }

        return ticketsDto;
    }
}
