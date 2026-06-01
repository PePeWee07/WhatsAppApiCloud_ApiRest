package com.BackEnd.WhatsappApiCloud.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.BackEnd.WhatsappApiCloud.model.dto.user.UserChatSessionDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.AiResponseDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageAddresDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageErrorDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessagePricingDto;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage.MessageRowView;
import com.BackEnd.WhatsappApiCloud.service.userChat.UserChatSessionService;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.MessageHistoryService;

@RequestMapping("/api/v1/messages")
@RestController
public class MessageHistoryController {

    private final MessageHistoryService historyService;
    private final UserChatSessionService sessionService;

    public MessageHistoryController(MessageHistoryService historyService, UserChatSessionService sessionService) {
        this.historyService = historyService;
        this.sessionService = sessionService;
    }

    /*
     * Listar las sesiones de chat (días con actividad) de un usuario.
     */
    @GetMapping("/sessions")
    public List<UserChatSessionDto> getSessions(@RequestParam String phone) {
        return sessionService.listSessions(phone);
    }

    /*
     * Cargar el historial de un usuario en un rango de fechas [start, end].
     * start/end en formato ISO local (ej. 2026-05-30T00:00:00); se interpretan en zona America/Guayaquil.
     */
    @GetMapping("/history/range")
    public Page<MessageRowView> getHistoryByRange(
            @RequestParam String phone,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "asc") String direction) {
        return historyService.getHistoryByPhoneAndRange(phone, start, end, page, size, direction);
    }

    /* 
     * Obener todo el historial de mensajes de un numero de whatsapp, con paginacion y ordenamiento
     * @param phone numero de whatsapp
     * @param page numero de pagina
     * @param size cantidad de registros por pagina
     * @param direction ordenamiento (asc o desc)
     */
    @GetMapping("/history")
    public Page<MessageRowView> getHistory(
        @RequestParam String phone,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(defaultValue = "desc") String direction) {
        return historyService.getHistoryByPhone(phone, page, size, direction);
    }

    /*
     * Obtener el detalle de facturacion por mensaje
     * Solo aplica para mensajes enviados por la empresa, no para mensajes recibidos por el cliente 
    */ 
    @GetMapping("/{id}/pricing")
    public MessagePricingDto getPricing(@PathVariable("id") Long messageId) {
        if (messageId == null || messageId <= 0) {
            throw new IllegalArgumentException("Invalid message ID");
        }
        return historyService.getMessagePricingByMessageId(messageId);
    }

    /*  
    * Obtener el detalle del error de un mensaje, si es que existio un error en el envio
    */
    @GetMapping("/{id}/error")
    public MessageErrorDto getError(@PathVariable("id") Long messageId) {
        if (messageId == null || messageId <= 0) {
            throw new IllegalArgumentException("Invalid message ID");
        }
        return historyService.getMessageErrorByMessageId(messageId);
    }

    /* 
     * Obtener el detalle de la direccion del mensaje
     */
    @GetMapping("/{id}/address")
    public MessageAddresDto getAddres(@PathVariable("id") Long messageId)
    {
        if (messageId == null || messageId <= 0) {
            throw new IllegalArgumentException("Invalid message ID");
        }

        return historyService.getMessageAddresByMessageId(messageId);
    }

    /* 
     * Obtener el detalle de respuesta de la IA para un mensaje, si es que se realizo una llamada a la IA en el proceso de envio del mensaje
    */
    @GetMapping("/{id}/ai-response")
    public List<AiResponseDto> getAiResponses(@PathVariable("id") Long messageId) {
        return historyService.getAiResponsesByMessageId(messageId);
    }

    /* 
    * Obtener el detalle completo de un mensaje, incluyendo su informacion basica, facturacion, error, direccion y respuestas de IA
    */
    @GetMapping("/{id}")
    public MessageDto getMessageDetails(@PathVariable("id") Long messageId) {
        return historyService.getMessageDetailsById(messageId);
    }

}
