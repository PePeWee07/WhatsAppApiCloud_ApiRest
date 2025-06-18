package com.BackEnd.WhatsappApiCloud.model.dto.glpi;

import java.util.List;

public record TicketInfoDto(
        String requester_email,
        List<String> watcher_emails,
        List<TicketInfoDto.TechDto> assigned_techs,
        TicketInfoDto.TicketDto ticket,
        List<TicketInfoDto.TicketSolutionDto> solutions,
        List<TicketInfoDto.NoteDto> notes) {
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
            Object users_id_supervisor) {
    }

    public static record TicketDto(
            Long id,
            String name,
            String closedate,
            String solvedate,
            String date_mod,
            Object status,
            String content,
            String urgency,
            String impact,
            String priority,
            String itilcategories_id,
            String type,
            String locations_id,
            String date_creation) {
    }

    public static record TicketSolutionDto(
            String content,
            String date_creation,
            String status,
            List<String> mediaIds) {
    }

    public static record NoteDto(
            String date_creation,
            String content,
            List<String> mediaIds) {
    }

    // Todo: List<MediaFileDto> mediaFiles
    public static record MediaFileDto(
            String mediaId,
            String name) {
    }

}
