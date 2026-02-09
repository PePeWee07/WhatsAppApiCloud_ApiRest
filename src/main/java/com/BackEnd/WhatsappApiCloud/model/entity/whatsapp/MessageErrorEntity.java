package com.BackEnd.WhatsappApiCloud.model.entity.whatsapp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "messages_error")
public class MessageErrorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String errorCode;
    private String errorTitle;
    @Column(columnDefinition = "TEXT")
    private String errorDetails;
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false, unique = true)
    private MessageEntity message;
}
