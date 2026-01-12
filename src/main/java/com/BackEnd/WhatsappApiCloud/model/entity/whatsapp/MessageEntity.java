package com.BackEnd.WhatsappApiCloud.model.entity.whatsapp;

import java.time.Instant;

import com.BackEnd.WhatsappApiCloud.util.enums.MessageDirectionEnum;
import com.BackEnd.WhatsappApiCloud.util.enums.MessageSourceEnum;
import com.BackEnd.WhatsappApiCloud.util.enums.MessageTypeEnum;

import jakarta.persistence.*;
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
@Table(name = "messages")
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String messageId;
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
    @Column(columnDefinition = "TEXT")
    private String caption;

    private String relatedMessageId;

    private String mimeType;
    private String mediaId;
    private String mediaFilename;

    private Double latitude;
    private Double longitude;
    private String locationName;
    private String locationAddress;

    private String reactionEmoji;

    private Instant sentAt;
    private Instant deliveredAt;
    private Instant readAt;
    private Instant failedAt;

    private Boolean billable;
    private String pricingModel;
    private String pricingCategory;
    private String pricingType;

    private String errorCode;
    private String errorTitle;
    @Column(columnDefinition = "TEXT")
    private String errorDetails;
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "from_phone", nullable = false)
    private String fromPhone;

    @Column(name = "to_phone", nullable = false)
    private String toPhone;

    // Foregin Key to Template Message
    @OneToOne(mappedBy = "message", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private MessageTemplateEntity messageTemplate;
}
