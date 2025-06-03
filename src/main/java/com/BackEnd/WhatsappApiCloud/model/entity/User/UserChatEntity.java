package com.BackEnd.WhatsappApiCloud.model.entity.user;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
@Table(name = "user_chat")
public class UserChatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "whatsapp_phone", unique = true, nullable = false)
    private String whatsappPhone;

    @Column(name = "thread_id")
    private String threadId;

    @Column(name = "limit_questions")
    private int limitQuestions;

    @Column(name = "first_interaction")
    private LocalDateTime firstInteraction;

    @Column(name = "last_interaction")
    private LocalDateTime lastInteraction;

    @Column(name = "next_reset_date")
    private LocalDateTime nextResetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_state")
    private ConversationState conversationState;

    @Column(name = "limit_strike")
    private int limitStrike;

    @Column(name = "block")
    private boolean block;

    @Column(name = "blocking_reason")
    private String blockingReason;

    @Column(name = "valid_question_count")
    private int validQuestionCount;

    @OneToMany(mappedBy = "userChat",
           cascade = CascadeType.ALL,
           fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    @JsonManagedReference
    private List<ChatSession> chatSessions;

    // ----- Campo ERP básico -----

    @Column(name = "identificacion")
    private String identificacion;
}
