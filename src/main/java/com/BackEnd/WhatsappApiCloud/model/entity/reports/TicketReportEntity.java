package com.BackEnd.WhatsappApiCloud.model.entity.reports;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Data
@Entity
@Table(name = "ticket_report")
@NoArgsConstructor
@AllArgsConstructor
public class TicketReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "user_requester", nullable = false)
    private String userRequester;

    @Column(name = "name_ticket", nullable = false)
    private String nameTicket;
    
}
