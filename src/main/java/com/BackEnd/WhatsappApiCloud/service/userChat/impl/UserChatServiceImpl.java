package com.BackEnd.WhatsappApiCloud.service.userChat.impl;

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
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.UserTicketDto;
import com.BackEnd.WhatsappApiCloud.model.dto.user.ChatSessionDto;
import com.BackEnd.WhatsappApiCloud.model.dto.user.UserChatFullDto;
import com.BackEnd.WhatsappApiCloud.model.entity.glpi.UserTicketEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatEntity;
import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.BackEnd.WhatsappApiCloud.repository.UserChatRepository;
import com.BackEnd.WhatsappApiCloud.repository.UserTicketRepository;
import com.BackEnd.WhatsappApiCloud.service.erp.ErpJsonServerClient;
import com.BackEnd.WhatsappApiCloud.service.glpi.GlpiService;
import com.BackEnd.WhatsappApiCloud.service.userChat.UserchatService;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.ApiWhatsappService;
import com.fasterxml.jackson.core.JsonProcessingException;

@Service
public class UserChatServiceImpl implements UserchatService {

    private final UserChatRepository repo;
    private final UserTicketRepository  userTicketRepository;
    private final ErpJsonServerClient erpClient;
    private final GlpiService glpiService;
    private final ApiWhatsappService apiWhatsappService;


    public UserChatServiceImpl(UserChatRepository repo, ErpJsonServerClient erpClient, UserTicketRepository  userTicketRepository, GlpiService glpiService, ApiWhatsappService apiWhatsappService) {
        this.erpClient = erpClient;
        this.repo = repo;
        this.userTicketRepository = userTicketRepository;
        this.glpiService = glpiService;
        this.apiWhatsappService = apiWhatsappService;
    }

    // ======================================================
    //   Buscar usuario por identificacion o whatsappPhone
    // ======================================================
    @Override
    @Transactional(readOnly = true)
    public UserChatFullDto findByIdentificacion(String identificacion) {
        UserChatEntity user = repo.findByIdentificacion(identificacion)
            .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario con identificacion: " + identificacion));

        ErpUserDto erpUser = erpClient.getUser(identificacion);

        List<ChatSessionDto> sesionesDto = user.getChatSessions().stream()
            .map(cs -> new ChatSessionDto(
                cs.getId(),
                cs.getWhatsappPhone(),
                cs.getMessageCount(),
                cs.getStartTime(),
                cs.getEndTime()))
            .collect(Collectors.toList());
        
        List<UserTicketDto> ticketsDto = user.getTickets().stream()
            .map(ticket -> new UserTicketDto(
                ticket.getId(),
                ticket.getWhatsappPhone(),
                ticket.getName(),
                ticket.getStatus(),
                ticket.getDate_creation(),
                ticket.getClosedate(),
                ticket.getSolvedate(),
                ticket.getDate_mod()
                )).collect(Collectors.toList());

        UserChatFullDto fullDto = new UserChatFullDto();
        fullDto.setId(user.getId());
        fullDto.setWhatsappPhone(user.getWhatsappPhone());
        fullDto.setThreadId(user.getThreadId());
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

        fullDto.setErpUser(erpUser);

        return fullDto;
    }


    @Override
    @Transactional(readOnly = true)
    public UserChatFullDto findByWhatsappPhone(String whatsappPhone) {
        UserChatEntity user = repo.findByWhatsappPhone(whatsappPhone)
            .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario con whatsappPhone: " + whatsappPhone));
        
        String identificacion = user.getIdentificacion();

        ErpUserDto erpUser;
        erpUser = erpClient.getUser(identificacion);

        List<ChatSessionDto> sesionesDto = user.getChatSessions().stream()
            .map(cs -> new ChatSessionDto(
                cs.getId(),
                cs.getWhatsappPhone(),
                cs.getMessageCount(),
                cs.getStartTime(),
                cs.getEndTime()))
            .collect(Collectors.toList());
        List<UserTicketDto> ticketsDto = user.getTickets().stream()
            .map(ticket -> new UserTicketDto(
                ticket.getId(),
                ticket.getWhatsappPhone(),
                ticket.getName(),
                ticket.getStatus(),
                ticket.getDate_creation(),
                ticket.getClosedate(),
                ticket.getSolvedate(),
                ticket.getDate_mod()
                )).collect(Collectors.toList());

        UserChatFullDto fullDto = new UserChatFullDto();
        fullDto.setId(user.getId());
        fullDto.setWhatsappPhone(user.getWhatsappPhone());
        fullDto.setThreadId(user.getThreadId());
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

        fullDto.setErpUser(erpUser);

        return fullDto;
    }

    // ======================================================
    //   Paginar todos los usuarios
    // ======================================================
    @Override
    @Transactional(readOnly = true)
    public Page<UserChatFullDto> usersTable(int page, int size, String sortBy, String direction) {

        Sort sort = Sort.by(sortBy);
        sort = "desc".equalsIgnoreCase(direction) ? sort.descending() : sort.ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserChatEntity> pageLocal = repo.findAll(pageable);

        List<UserChatFullDto> dtos = pageLocal.getContent().stream()
            .map(user -> {
                ErpUserDto erpUser;
                erpUser = erpClient.getUser(user.getIdentificacion());

                List<ChatSessionDto> sesionesDto = user.getChatSessions().stream()
                    .map(cs -> new ChatSessionDto(
                        cs.getId(),
                        cs.getWhatsappPhone(),
                        cs.getMessageCount(),
                        cs.getStartTime(),
                        cs.getEndTime()))
                    .collect(Collectors.toList());
                
                List<UserTicketDto> ticketsDto = user.getTickets().stream()
                    .map(ticket -> new UserTicketDto(
                        ticket.getId(),
                        ticket.getWhatsappPhone(),
                        ticket.getName(),
                        ticket.getStatus(),
                        ticket.getDate_creation(),
                        ticket.getClosedate(),
                        ticket.getSolvedate(),
                        ticket.getDate_mod()
                        )).collect(Collectors.toList());

                UserChatFullDto fullDto = new UserChatFullDto();
                fullDto.setId(user.getId());
                fullDto.setWhatsappPhone(user.getWhatsappPhone());
                fullDto.setThreadId(user.getThreadId());
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
                

                fullDto.setErpUser(erpUser);
                return fullDto;
            })
            .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, pageLocal.getTotalElements());
    }

    // ======================================================
    //   Buscar usuarios por ultima interaccion
    // ======================================================
    @Override
    @Transactional(readOnly = true)
    public Page<UserChatFullDto> tablefindByLastInteraction(int page, int size, String sortBy, String direction, LocalDateTime inicio, LocalDateTime fin) {

        Sort sort = Sort.by(sortBy);
        sort = "desc".equalsIgnoreCase(direction) ? sort.descending() : sort.ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserChatEntity> pageLocal = repo.findByThreadIdIsNotNullAndLastInteractionBetween(inicio, fin, pageable);

        List<UserChatFullDto> dtos = pageLocal.getContent().stream()
            .map(user -> {
                ErpUserDto erpUser;
                erpUser = erpClient.getUser(user.getIdentificacion());

                List<ChatSessionDto> sesionesDto = user.getChatSessions().stream()
                    .map(cs -> new ChatSessionDto(
                        cs.getId(),
                        cs.getWhatsappPhone(),
                        cs.getMessageCount(),
                        cs.getStartTime(),
                        cs.getEndTime()))
                    .collect(Collectors.toList());
                List<UserTicketDto> ticketsDto = user.getTickets().stream()
            .map(ticket -> new UserTicketDto(
                ticket.getId(),
                ticket.getWhatsappPhone(),
                ticket.getName(),
                ticket.getStatus(),
                ticket.getDate_creation(),
                ticket.getClosedate(),
                ticket.getSolvedate(),
                ticket.getDate_mod()
                )).collect(Collectors.toList());

                UserChatFullDto fullDto = new UserChatFullDto();
                fullDto.setId(user.getId());
                fullDto.setWhatsappPhone(user.getWhatsappPhone());
                fullDto.setThreadId(user.getThreadId());
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

                fullDto.setErpUser(erpUser);
                return fullDto;
            })
            .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, pageLocal.getTotalElements());
    }

    // ======================================================
    //   Buscar usuarios por fecha de inicio de sesion
    // ======================================================
    @Override
    @Transactional(readOnly = true)
    public Page<UserChatFullDto> tablefindByChatSessionStart(int page, int size, String sortBy, String direction, LocalDateTime inicio, LocalDateTime fin) {

        Sort sort = Sort.by(sortBy);
        sort = "desc".equalsIgnoreCase(direction) ? sort.descending() : sort.ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserChatEntity> pageLocal = repo.findDistinctByChatSessionsStartTimeBetween(inicio, fin, pageable);

        List<UserChatFullDto> dtos = pageLocal.getContent().stream()
            .map(user -> {
                ErpUserDto erpUser;
                erpUser = erpClient.getUser(user.getIdentificacion());

                List<ChatSessionDto> sesionesDto = user.getChatSessions().stream()
                    .map(cs -> new ChatSessionDto(
                        cs.getId(),
                        cs.getWhatsappPhone(),
                        cs.getMessageCount(),
                        cs.getStartTime(),
                        cs.getEndTime()))
                    .collect(Collectors.toList());
                    List<UserTicketDto> ticketsDto = user.getTickets().stream()
            .map(ticket -> new UserTicketDto(
                ticket.getId(),
                ticket.getWhatsappPhone(),
                ticket.getName(),
                ticket.getStatus(),
                ticket.getDate_creation(),
                ticket.getClosedate(),
                ticket.getSolvedate(),
                ticket.getDate_mod()
                )).collect(Collectors.toList());

                UserChatFullDto fullDto = new UserChatFullDto();
                fullDto.setId(user.getId());
                fullDto.setWhatsappPhone(user.getWhatsappPhone());
                fullDto.setThreadId(user.getThreadId());
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

                fullDto.setErpUser(erpUser);
                return fullDto;
            })
            .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, pageLocal.getTotalElements());
    }

    // ======================================================
    //   Actualizar datos de usuario
    // ======================================================
    @Override
    @Transactional
    public UserChatFullDto userUpdate(Long id, Map<String, Object> updates) {
        UserChatEntity user = repo.findById(id)
            .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con id " + id));

        if (updates.containsKey("threadId")) {
            Object threadVal = updates.get("threadId");
            if (threadVal instanceof String) {
                user.setThreadId((String) threadVal);
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

        UserChatEntity saved = repo.save(user);

        saved.getChatSessions().size();

        ErpUserDto erpUser;
        erpUser = erpClient.getUser(saved.getIdentificacion());

        List<ChatSessionDto> sesionesDto = saved.getChatSessions().stream()
            .map(cs -> new ChatSessionDto(
                cs.getId(),
                cs.getWhatsappPhone(),
                cs.getMessageCount(),
                cs.getStartTime(),
                cs.getEndTime()))
            .collect(Collectors.toList());
         List<UserTicketDto> ticketsDto = saved.getTickets().stream()
        .map(ticket -> new UserTicketDto(
                ticket.getId(),
                ticket.getWhatsappPhone(),
                ticket.getName(),
                ticket.getStatus(),
                ticket.getDate_creation(),
                ticket.getClosedate(),
                ticket.getSolvedate(),
                ticket.getDate_mod()
                )).collect(Collectors.toList());

        UserChatFullDto fullDto = new UserChatFullDto();
        fullDto.setId(saved.getId());
        fullDto.setWhatsappPhone(saved.getWhatsappPhone());
        fullDto.setThreadId(saved.getThreadId());
        fullDto.setLimitQuestions(saved.getLimitQuestions());
        fullDto.setFirstInteraction(saved.getFirstInteraction());
        fullDto.setLastInteraction(saved.getLastInteraction());
        fullDto.setNextResetDate(saved.getNextResetDate());
        fullDto.setConversationState(saved.getConversationState().name());
        fullDto.setLimitStrike(saved.getLimitStrike());
        fullDto.setBlock(saved.isBlock());
        fullDto.setBlockingReason(saved.getBlockingReason());
        fullDto.setValidQuestionCount(saved.getValidQuestionCount());
        fullDto.setChatSessions(sesionesDto);
        fullDto.setUserTickets(ticketsDto);

        fullDto.setErpUser(erpUser);

        return fullDto;
    }

    @Override
    public List<UserTicketDto> listOpenTickets(String whatsAppPhone) {
            repo.findByWhatsappPhone(whatsAppPhone)
            .orElseThrow(() ->
                    new UserNotFoundException(
                    "Usuario no encontrado para el número de WhatsApp: " + whatsAppPhone)
            );

            List<UserTicketEntity> entities = userTicketRepository.findByWhatsappPhoneAndStatusNot(whatsAppPhone, "Cerrado");

            return entities.stream()
            .map(e -> new UserTicketDto(
                    e.getId(),
                    e.getWhatsappPhone(),
                    e.getName(),
                    e.getStatus(),
                    e.getDate_creation(),
                    e.getClosedate(),
                    e.getSolvedate(),
                    e.getDate_mod()
            ))
            .toList();
    }

    // ---------- Usuario solicita info del Ticket ----------
    @Override
    public void userRequest(String whatsAppPhone, String ticketId) throws JsonProcessingException {

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
                sb.append("- ").append(tech.firstname()).append(" ").append(tech.realname());
                if (tech.mobile() != null) {
                    sb.append(" (").append(tech.mobile()).append(")");
                }
                sb.append("\n");
            }
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
                sb.append("\n").append("> *Solucionado:*\n");
                sb.append("`Fecha de resolución:` ").append(solution.date_creation()).append("\n\n");
                sb.append(solution.content()).append("\n");

                // Enviar imágenes asociadas a la solución
                if (solution.mediaIds() != null && !solution.mediaIds().isEmpty()) {
                    for (String mediaId : solution.mediaIds()) {
                        apiWhatsappService.sendImageMessageById(whatsAppPhone, mediaId, "Solución del Ticket");
                    }
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
            if (lastNote.mediaIds() != null && !lastNote.mediaIds().isEmpty()) {
                for (String mediaId : lastNote.mediaIds()) {
                    apiWhatsappService.sendImageMessageById(whatsAppPhone, mediaId, "Seguimiento del Ticket");
                }
            }
        }

         // 4) Dividir el mensaje si excede el límite de 4096 caracteres
        String message = sb.toString();
        if (message.length() > 4096) {
            List<String> parts = splitMessage(message, 4096);
            for (String part : parts) {
                apiWhatsappService.sendMessage(new MessageBody(whatsAppPhone, part));
            }
        } else {
            apiWhatsappService.sendMessage(new MessageBody(whatsAppPhone, message));
        }
    }

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
}
