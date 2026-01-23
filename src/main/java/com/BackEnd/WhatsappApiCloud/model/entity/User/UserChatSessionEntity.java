package com.BackEnd.WhatsappApiCloud.model.entity.user;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "chat_sessions")
@NoArgsConstructor
@AllArgsConstructor
public class UserChatSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "whatsapp_phone", nullable = false)
    private String whatsappPhone;

    @Column(name = "message_count", nullable = false)
    private int messageCount = 0;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "whatsapp_phone", referencedColumnName = "whatsapp_phone", insertable = false, updatable = false)
    @JsonBackReference
    private UserChatEntity userChat;

}
