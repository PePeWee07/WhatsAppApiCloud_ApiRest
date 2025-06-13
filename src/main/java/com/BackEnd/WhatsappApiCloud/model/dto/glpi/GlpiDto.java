package com.BackEnd.WhatsappApiCloud.model.dto.glpi;

import java.util.List;

public class GlpiDto {

        public record UserTicket(
                        Long id,
                        Long tickets_id,
                        Long users_id,
                        Long type,
                        Long use_notification,
                        String alternative_email,
                        Link[] links) {
        }

        public record UserGlpi(
                        Long id,
                        String name,
                        String password_last_update,
                        String phone,
                        String phone2,
                        String mobile,
                        String realname,
                        String firstname,
                        String locations_id,
                        String language,
                        Long use_mode,
                        Object list_limit,
                        Long is_active,
                        String comment,
                        String auths_id,
                        Long authtype,
                        String last_login,
                        String date_mod,
                        String date_sync,
                        Long is_deleted,
                        String profiles_id,
                        String entities_id,
                        Object usertitles_id,
                        Long usercategories_id,
                        Object date_format,
                        Object number_format,
                        Object names_format,
                        Object csv_delimiter,
                        Object is_ids_visible,
                        Object use_flat_dropdowntree,
                        Object show_jobs_at_login,
                        Object priority_1,
                        Object priority_2,
                        Object priority_3,
                        Object priority_4,
                        Object priority_5,
                        Object priority_6,
                        Object followup_private,
                        Object task_private,
                        Object default_requesttypes_id,
                        Object password_forget_token,
                        Object password_forget_token_date,
                        String user_dn,
                        Object registration_number,
                        Object show_count_on_tabs,
                        Object refresh_views,
                        Object set_default_tech,
                        Object personal_token_date,
                        Object api_token_date,
                        String cookie_token_date,
                        Object display_count_on_home,
                        Object notification_to_myself,
                        Object duedateok_color,
                        Object duedatewarning_color,
                        Object duedatecritical_color,
                        Object duedatewarning_less,
                        Object duedatecritical_less,
                        Object duedatewarning_unit,
                        Object duedatecritical_unit,
                        Object display_options,
                        Long is_deleted_ldap,
                        Object pdffont,
                        String picture,
                        String begin_date,
                        String end_date,
                        Object keep_devices_when_purging_item,
                        Object privatebookmarkorder,
                        Object backcreated,
                        Object task_state,
                        Object layout,
                        Object palette,
                        Object set_default_requester,
                        Object lock_autolock_mode,
                        Object lock_directunlock_notification,
                        String date_creation,
                        Long highcontrast_css,
                        Object plannings,
                        String sync_field,
                        String groups_id,
                        Object users_id_supervisor,
                        String timezone,
                        Object default_dashboard_central,
                        Object default_dashboard_assets,
                        Object default_dashboard_helpdesk,
                        Object default_dashboard_mini_ticket,
                        Link[] links) {
        }

        public record Ticket(
                        Long id,
                        String entities_id,
                        String name,
                        String date,
                        String closedate,
                        String solvedate,
                        String date_mod,
                        String users_id_lastupdater,
                        Long status,
                        Object users_id_recipient,
                        String requesttypes_id,
                        String content,
                        Long urgency,
                        Long impact,
                        Long priority,
                        String itilcategories_id,
                        Long type,
                        Long global_validation,
                        Long slas_id_ttr,
                        Long slas_id_tto,
                        Long slalevels_id_ttr,
                        String time_to_resolve,
                        String time_to_own,
                        String begin_waiting_date,
                        Long sla_waiting_duration,
                        Long ola_waiting_duration,
                        Long olas_id_tto,
                        Long olas_id_ttr,
                        Long olalevels_id_ttr,
                        Object ola_ttr_begin_date,
                        Object internal_time_to_resolve,
                        Object internal_time_to_own,
                        Long waiting_duration,
                        Long close_delay_stat,
                        Long solve_delay_stat,
                        Long takeintoaccount_delay_stat,
                        Long actiontime,
                        Long is_deleted,
                        String locations_id,
                        Long validation_percent,
                        String date_creation,
                        Document[] _documents,
                        Tickets[] _tickets,
                        Notes _notes,
                        Link[] links) {
        }

        public record Document(
                        Long assocID,
                        String assocdate,
                        Long entityID,
                        String entity,
                        String headings,
                        Long id,
                        String entities_id,
                        Long is_recursive,
                        String name,
                        String filename,
                        String filepath,
                        Long documentcategories_id,
                        String mime,
                        String date_mod,
                        String comment,
                        Long is_deleted,
                        String link,
                        Object users_id,
                        Long tickets_id,
                        String sha1sum,
                        Long is_blacklisted,
                        String tag,
                        String date_creation,
                        Link[] links) {
        }

        public record Document_Item(
                        Long id,
                        String documents_id,
                        Object items_id,
                        Object itemtype,
                        Object entities_id,
                        Object is_recursive,
                        Object date_mod,
                        Object users_id,
                        Object timeline_position,
                        Object date_creation,
                        Link[] links) {
        }

        public record Tickets(
                        Ticket _tickets) {
        }

        public record Notes(
                        Integer error,
                        String message) {
        }

        // Seguimiento de tickets, se agg este record debido a la GLPI 9.5.3 tiene un
        // error 401 aun siendo super-admin y hay que siempre consultalo por separado si
        // exiten seguimientos
        public record TicketFollowUp(
                        Long id,
                        String itemtype,
                        String items_id,
                        String date,
                        String users_id,
                        Long users_id_editor,
                        String content,
                        Long is_private,
                        String requesttypes_id,
                        String date_mod,
                        String date_creation,
                        Long timeline_position,
                        Long sourceitems_id,
                        Long sourceof_items_id,
                        Link[] links) {
        }

        public record TicketSolution (
                Long id,
                String itemtype,
                String items_id,
                Object solutiontypes_id,
                Object solutiontype_name,
                String content,
                String date_creation,
                Object date_mod,
                Object date_approval,
                Object users_id,
                Object user_name,
                Object users_id_editor,
                Object users_id_approval,
                Object user_name_approval,
                Long status,
                Object itilfollowups_id,
                Link[] links
        ) {}

        public record Link(
                        String rel,
                        String href) {
        }

        public record CreateTicket (
                Input input
        ) {}
        public record Input(
                String name,
                String content,
                Object entities_id,
                Long requesttypes_id,
                Long _users_id_requester,
                UserIdRequesterNotif _users_id_requester_notif,
                Long users_id_lastupdater
        ) {}
        public record UserIdRequesterNotif(
                Long use_notification,
                List<String> alternative_email
        ) {}

        public record responseCreateTicketSuccess(
                Long id,
                String message
        ) {}
}