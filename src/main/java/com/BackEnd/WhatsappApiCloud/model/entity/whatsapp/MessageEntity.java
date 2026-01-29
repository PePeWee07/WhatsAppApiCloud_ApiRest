package com.BackEnd.WhatsappApiCloud.model.entity.whatsapp;

import java.time.Instant;
import java.util.Set;

import com.BackEnd.WhatsappApiCloud.model.entity.openIA.AiResponseEntity;
import com.BackEnd.WhatsappApiCloud.util.enums.MessageDirectionEnum;
import com.BackEnd.WhatsappApiCloud.util.enums.MessageSourceEnum;
import com.BackEnd.WhatsappApiCloud.util.enums.MessageTypeEnum;

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
@Table(name = "messages")
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String wamid;
    private String relatedWamid;
    
    @Column(name = "conversation_user_phone", nullable = false)
    private String conversationUserPhone;

    @Enumerated(EnumType.STRING)
    private MessageDirectionEnum direction;
    
    private String profileName;

    @Enumerated(EnumType.STRING)
    private MessageSourceEnum source;

    @Enumerated(EnumType.STRING)
    private MessageTypeEnum type;

    private Instant timestamp;
    @Column(columnDefinition = "TEXT")
    private String textBody;
    
    private String mediaMimeType;
    private String mediaId;
    private String mediaFilename;
    @Column(columnDefinition = "TEXT")
    private String mediaCaption;

    private String reactionEmoji;

    private Instant sentAt;
    private Instant deliveredAt;
    private Instant readAt;
    private Instant failedAt;


    @Column(name = "from_phone", nullable = false)
    private String fromPhone;

    @Column(name = "to_phone", nullable = false)
    private String toPhone;

    // Foregin Keys
    @OneToOne(mappedBy = "message", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private MessageTemplateEntity messageTemplate;

    @OneToMany(mappedBy = "message", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<AiResponseEntity> aiResponses;

    @OneToOne(mappedBy = "message", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private MessagePircingEntity messagePircingEntity;

    @OneToOne(mappedBy = "message", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private MessageAddresEntity messageAddresEntity;

    @OneToOne(mappedBy = "message", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private MessageErrorEntity messageErrorEntity;
}
