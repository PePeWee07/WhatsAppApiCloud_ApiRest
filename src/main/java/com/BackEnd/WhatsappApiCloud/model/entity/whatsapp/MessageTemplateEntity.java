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
@Table(name = "messages_templates")
public class MessageTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_name", nullable = false)
    private String templateName;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "answer", length = 700)
    private String answer;

    @Column(name = "message_status")
    private String messageStatus;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false, unique = true)
    private MessageEntity message;

}
