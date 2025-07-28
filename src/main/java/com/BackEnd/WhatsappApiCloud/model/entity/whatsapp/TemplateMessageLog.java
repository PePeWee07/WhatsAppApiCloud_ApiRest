package com.BackEnd.WhatsappApiCloud.model.entity.whatsapp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "template_message_log")
public class TemplateMessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "to_phone", nullable = false)
    private String toPhone;

    @Column(name = "template_name", nullable = false)
    private String templateName;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "wamid", nullable = false, length = 200)
    private String wamid;

    @Column(name = "answer", length = 700)
    private String answer;

    @Column(name = "message_status")
    private String messageStatus;
    
}
