package com.BackEnd.WhatsappApiCloud.model.dto.glpi;

import java.util.List;

public record TicketInfoDto(
    String requester_email,
    List<String> watcher_emails,
    List<TicketInfoDto.TechDto> assigned_techs,
    TicketInfoDto.TicketDto ticket,
    List<TicketInfoDto.NoteDto> notes
) {
    public static record TechDto(
        Long id,
        String name,
        String mobile,
        String realname,
        String firstname,
        String locations_id,
        Long is_active,
        String profiles_id,
        Object usertitles_id,
        String groups_id,
        Object users_id_supervisor
    ) {}

    public static record TicketDto(
        Long id,
        String name,
        String closedate,
        String solvedate,
        String date_mod,
        Long status,
        String content,
        Long urgency,
        Long impact,
        Long priority,
        String itilcategories_id,
        Long type,
        String locations_id,
        String date_creation
    ) {}

    public static record NoteDto(
        String date_creation,
        String content
    ) {}
}

// Obtener el userTicket (links, type, alternative_email)
// Si esta un tecnico asignado, consultar usaurio (id, name, mobile, realname, firstname, locations_id, is_active, profiles_id, usertitles_id, groups_id, users_id_supervisor)
// Obtener el ticket (id, name, closedate, solvedate, date_mod, status, content, urgency, impact, priority, itilcategories_id, type(incidencia, solicitud), locations_id, date_creation, _notes)
// Verificar si tiene _notes(date_creation, content)
// Devolver datos