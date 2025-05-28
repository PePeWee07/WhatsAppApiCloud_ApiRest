package com.BackEnd.WhatsappApiCloud.model.entity.user;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    // Atributos Necesarios
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 1, max = 100, message = "El nombre de usuario debe tener entre 1 y 100 caracteres")
    @Column(name = "names", length = 100, nullable = false)
    private String nombres;

    @Column(name = "cedula")
    private String cedula;

    @Column(name = "phone", unique = true, nullable = false)
    private String phone;

    @Column(name = "rol")
    private String rol;

    @Column(name = "thread_id")
    private String threadId;

    @Column(name = "limit_questions")
    private int limitQuestions;

    @Column(name = "firstInteraction")
    private LocalDateTime firstInteraction;
    
    @Column(name = "last_interaction")
    private LocalDateTime lastInteraction;
    
    @Column(name = "next_reset_date")
    private LocalDateTime nextResetDate;

    @Column(name = "conversation_state")
    private String conversationState;

    @Column(name = "limit_strike")
    private int limitStrike;

    @Column(name = "block")
    private boolean block;

    @Column(name = "blocking_reason")
    private String blockingReason;

    @Column(name = "email")
    private String email;

    @Column (name = "valid_question_count")
    private int validQuestionCount;

    // Atributos Adicionales
    private String sede;

    private String carrera;

    @OneToMany(mappedBy = "userChat",
           cascade = CascadeType.ALL,
           fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    @JsonManagedReference
    private List<ChatSession> chatSessions;

}
