package com.BackEnd.WhatsappApiCloud.service.glpi.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.BackEnd.WhatsappApiCloud.model.dto.glpi.GlpiDto.*;
import com.BackEnd.WhatsappApiCloud.service.glpi.GlpiServerClient;
import com.BackEnd.WhatsappApiCloud.service.glpi.GlpiService;

@Service
public class GlpiServiceImpl implements GlpiService {

    private final GlpiServerClient glpiServerClient;

    public GlpiServiceImpl(GlpiServerClient glpiServerClient) {
        this.glpiServerClient = glpiServerClient;
    }

    @Override
    public String getInfoTicketById(String ticketId) {
        List<UserTicket> userTickets = glpiServerClient.getTicketUser(ticketId);

        // Link del ticket
        Optional<String> ticketLinkOpt = userTickets.stream()
            .filter(t -> t.type() == 1)
            .flatMap(t -> Arrays.stream(t.links()))
            .filter(link -> "Ticket".equalsIgnoreCase(link.rel()))
            .map(Link::href)
            .findFirst();

        // Solicitante (type=1)
        String requester = userTickets.stream()
                .filter(t -> t.type() == 1)
                .map(UserTicket::alternative_email)
                .findFirst()
                .orElse("—sin solicitante—");

        // Observadores (type=3)
        List<String> watchers = userTickets.stream()
                .filter(t -> t.type() == 3)
                .map(UserTicket::alternative_email)
                .toList();

        // Tecnicos asignados (type=2)
        List<UserTicket> assignedTechs = userTickets.stream()
                .filter(t -> t.type() == 2)
                .toList();

        if (assignedTechs.isEmpty()) {
             return "No hay técnicos asignados aún.";
        } else {
            // Datos de los técnicos asignados
            for (UserTicket ut : assignedTechs) {
                String userHref = Arrays.stream(ut.links())
                        .filter(link -> "User".equals(link.rel()))
                        .map(Link::href)
                        .findFirst()
                        .orElse(null);

                if (userHref != null) {
                    UserGlpi tech = glpiServerClient.getUserByLink(userHref);
                    Object techDetails = Arrays.asList(
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
                        tech.users_id_supervisor()
                    );
                    System.out.printf("Técnico Asignado: %s%n", techDetails);
                }
            }
            // Datos del ticket
            
        }

        System.out.printf("Solicitante: %s%nObservadores: %s%n", requester, watchers);
        System.out.printf("Link del Ticket: %s%n", ticketLinkOpt.orElse("—sin enlace—"));
        return null;

        // ? 1 Obtener el userTicket (link del ticket, type, alternative_email, tegnico asignado si hay)
        // ? 2 Verificar si esta asignado a un tecnico
        // ? 3 Si esta un tecnico asignado, consultar usaurio (mobile, realname(Apellidos), firstname, locations_id, is_active, profiles_id, usertitles_id, groups_id, users_id_supervisor)
        // ? 4 Obtener el ticket (name, date(Fecha de apertura), closedate, solvedate, date_mod, status, content, urgency, impact, priority, itilcategories_id, type(incidencia, solicitud), locations_id, date_creation, __tickets, _notes, links)
        // ? 5 Verificar si tiene _tickets encadenados
        // ? 6 Verificar si tiene _notes(date_creation, content)
        // ? 7 Verificar Estado del ticket(status)
        // ? 8 Devolver datos
    }

}
