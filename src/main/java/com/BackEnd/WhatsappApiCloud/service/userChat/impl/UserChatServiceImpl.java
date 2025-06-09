package com.BackEnd.WhatsappApiCloud.service.userChat.impl;

import java.time.LocalDateTime;
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
import com.BackEnd.WhatsappApiCloud.model.dto.user.ChatSessionDto;
import com.BackEnd.WhatsappApiCloud.model.dto.user.UserChatFullDto;
import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatEntity;
import com.BackEnd.WhatsappApiCloud.repository.UserChatRepository;
import com.BackEnd.WhatsappApiCloud.service.erp.ErpJsonServerClient;
import com.BackEnd.WhatsappApiCloud.service.userChat.UserchatService;

@Service
public class UserChatServiceImpl implements UserchatService {

    private final UserChatRepository repo;
    private final ErpJsonServerClient erpClient;

    public UserChatServiceImpl(UserChatRepository repo, ErpJsonServerClient erpClient) {
        this.erpClient = erpClient;
        this.repo = repo;
    }

    // ======================================================
    //   Buscar usuario por identificacion o whatsappPhone
    // ======================================================
    @Override
    @Transactional(readOnly = true)
    public UserChatFullDto findByIdentificacion(String identificacion) {
        UserChatEntity user = repo.findByIdentificacion(identificacion)
            .orElseThrow(() -> new UserNotFoundException("No se encontro el usuario con identificacion: " + identificacion));

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
            .map(usuarioLocal -> {
                ErpUserDto erpUser;
                erpUser = erpClient.getUser(usuarioLocal.getIdentificacion());

                List<ChatSessionDto> sesionesDto = usuarioLocal.getChatSessions().stream()
                    .map(cs -> new ChatSessionDto(
                        cs.getId(),
                        cs.getWhatsappPhone(),
                        cs.getMessageCount(),
                        cs.getStartTime(),
                        cs.getEndTime()))
                    .collect(Collectors.toList());

                UserChatFullDto fullDto = new UserChatFullDto();
                fullDto.setId(usuarioLocal.getId());
                fullDto.setWhatsappPhone(usuarioLocal.getWhatsappPhone());
                fullDto.setThreadId(usuarioLocal.getThreadId());
                fullDto.setLimitQuestions(usuarioLocal.getLimitQuestions());
                fullDto.setFirstInteraction(usuarioLocal.getFirstInteraction());
                fullDto.setLastInteraction(usuarioLocal.getLastInteraction());
                fullDto.setNextResetDate(usuarioLocal.getNextResetDate());
                fullDto.setConversationState(usuarioLocal.getConversationState().name());
                fullDto.setLimitStrike(usuarioLocal.getLimitStrike());
                fullDto.setBlock(usuarioLocal.isBlock());
                fullDto.setBlockingReason(usuarioLocal.getBlockingReason());
                fullDto.setValidQuestionCount(usuarioLocal.getValidQuestionCount());
                fullDto.setChatSessions(sesionesDto);

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
            .map(usuarioLocal -> {
                ErpUserDto erpUser;
                erpUser = erpClient.getUser(usuarioLocal.getIdentificacion());

                List<ChatSessionDto> sesionesDto = usuarioLocal.getChatSessions().stream()
                    .map(cs -> new ChatSessionDto(
                        cs.getId(),
                        cs.getWhatsappPhone(),
                        cs.getMessageCount(),
                        cs.getStartTime(),
                        cs.getEndTime()))
                    .collect(Collectors.toList());

                UserChatFullDto fullDto = new UserChatFullDto();
                fullDto.setId(usuarioLocal.getId());
                fullDto.setWhatsappPhone(usuarioLocal.getWhatsappPhone());
                fullDto.setThreadId(usuarioLocal.getThreadId());
                fullDto.setLimitQuestions(usuarioLocal.getLimitQuestions());
                fullDto.setFirstInteraction(usuarioLocal.getFirstInteraction());
                fullDto.setLastInteraction(usuarioLocal.getLastInteraction());
                fullDto.setNextResetDate(usuarioLocal.getNextResetDate());
                fullDto.setConversationState(usuarioLocal.getConversationState().name());
                fullDto.setLimitStrike(usuarioLocal.getLimitStrike());
                fullDto.setBlock(usuarioLocal.isBlock());
                fullDto.setBlockingReason(usuarioLocal.getBlockingReason());
                fullDto.setValidQuestionCount(usuarioLocal.getValidQuestionCount());
                fullDto.setChatSessions(sesionesDto);

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
            .map(usuarioLocal -> {
                ErpUserDto erpUser;
                erpUser = erpClient.getUser(usuarioLocal.getIdentificacion());

                List<ChatSessionDto> sesionesDto = usuarioLocal.getChatSessions().stream()
                    .map(cs -> new ChatSessionDto(
                        cs.getId(),
                        cs.getWhatsappPhone(),
                        cs.getMessageCount(),
                        cs.getStartTime(),
                        cs.getEndTime()))
                    .collect(Collectors.toList());

                UserChatFullDto fullDto = new UserChatFullDto();
                fullDto.setId(usuarioLocal.getId());
                fullDto.setWhatsappPhone(usuarioLocal.getWhatsappPhone());
                fullDto.setThreadId(usuarioLocal.getThreadId());
                fullDto.setLimitQuestions(usuarioLocal.getLimitQuestions());
                fullDto.setFirstInteraction(usuarioLocal.getFirstInteraction());
                fullDto.setLastInteraction(usuarioLocal.getLastInteraction());
                fullDto.setNextResetDate(usuarioLocal.getNextResetDate());
                fullDto.setConversationState(usuarioLocal.getConversationState().name());
                fullDto.setLimitStrike(usuarioLocal.getLimitStrike());
                fullDto.setBlock(usuarioLocal.isBlock());
                fullDto.setBlockingReason(usuarioLocal.getBlockingReason());
                fullDto.setValidQuestionCount(usuarioLocal.getValidQuestionCount());
                fullDto.setChatSessions(sesionesDto);

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
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id " + id));

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

        fullDto.setErpUser(erpUser);

        return fullDto;
    }


}
