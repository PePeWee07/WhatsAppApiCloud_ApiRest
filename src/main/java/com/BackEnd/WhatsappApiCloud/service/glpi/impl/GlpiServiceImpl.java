package com.BackEnd.WhatsappApiCloud.service.glpi.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import com.BackEnd.WhatsappApiCloud.exception.ServerClientException;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.GlpiDto.*;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.TicketInfoDto;
import com.BackEnd.WhatsappApiCloud.model.dto.glpi.TicketInfoDto.*;
import com.BackEnd.WhatsappApiCloud.service.glpi.GlpiServerClient;
import com.BackEnd.WhatsappApiCloud.service.glpi.GlpiService;

@Service
public class GlpiServiceImpl implements GlpiService {

    private final GlpiServerClient glpiServerClient;

    public GlpiServiceImpl(GlpiServerClient glpiServerClient) {
        this.glpiServerClient = glpiServerClient;
    }

    @Override
    public TicketInfoDto getInfoTicketById(String ticketId) {
        List<UserTicket> userTickets = glpiServerClient.getTicketUser(ticketId);

        // 1) Obtener link del ticket desde el solicitante (type=1)
        String ticketLink = userTickets.stream()
                .filter(t -> t.type() == 1)
                .flatMap(t -> Arrays.stream(t.links()))
                .filter(link -> "Ticket".equalsIgnoreCase(link.rel()))
                .map(link -> link.href())
                .findFirst()
                .orElseThrow(() -> new ServerClientException("No se encontró el enlace del ticket."));

        // 2) Solicitante
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
                            .orElseThrow(() -> new ServerClientException("No se encontró el enlace del usuario técnico."));
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
                glpiTicket.status(),
                Jsoup.parse(glpiTicket.content()).text(),
                glpiTicket.urgency(),
                glpiTicket.impact(),
                glpiTicket.priority(),
                glpiTicket.itilcategories_id(),
                glpiTicket.type(),
                glpiTicket.locations_id(),
                glpiTicket.date_creation());

        // 6) Seguimientos (_notes)
        List<TicketFollowUp> rawNotes = glpiServerClient.TicketWithNotes(glpiTicket.id());
        List<NoteDto> notes = rawNotes.stream()
                .map(n -> new NoteDto(
                        n.date_creation(),
                        Jsoup.parse(n.content()).text() // limpia HTML
                ))
                .collect(Collectors.toList());

        // 7) Construir y devolver DTO final
        return new TicketInfoDto(
                requester,
                watchers,
                techDtos,
                ticketDto,
                notes);
    }

}
