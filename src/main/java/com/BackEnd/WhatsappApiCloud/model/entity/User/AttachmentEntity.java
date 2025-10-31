package com.BackEnd.WhatsappApiCloud.model.entity.user;

import java.time.Instant;

import com.BackEnd.WhatsappApiCloud.util.enums.AttachmentStatus;
import com.BackEnd.WhatsappApiCloud.util.enums.ConversationState;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "attachment")
public class AttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== Atributos obtenidos de Whatsapp =====
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "attachment_id", nullable = false)
    private String attachmentID;

    @Column(name = "caption", length = 1350, nullable = true)
    private String caption; //! opcional
    
    
    // ===== Atributos obtenidos de GLPI al crear ticket =====
    @Column(name = "ticket_id", nullable = true)
    private Long ticketId; //! opcional

    @Column(name = "glpi_document_id", nullable = true)
    private Long gpliDocuemntId; //! opcional


    // ===== Atributos añadidos =====
    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_state", nullable = false)
    private ConversationState conversationState;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_status", nullable = false)
    private AttachmentStatus attachmentStatus = AttachmentStatus.UNUSED;


    // ===== Vinculacion con el Usuario =====
    @Column(name = "whatsapp_phone", nullable = false)
    private String whatsappPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "whatsapp_phone",
        referencedColumnName = "whatsapp_phone",
        insertable = false,
        updatable = false
    )
    @JsonBackReference
    private UserChatEntity userChat;
    
}
