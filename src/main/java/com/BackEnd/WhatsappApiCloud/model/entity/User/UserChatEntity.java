package com.BackEnd.WhatsappApiCloud.model.entity.User;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "rol")
    private String rol;

    @Column(name = "thread_id")
    private String threadId;

    @Column(name = "limit_questions")
    private int limitQuestions;

    @Column(name = "limit_questions_used")
    private LocalDateTime firstInteraction;
    
    @Column(name = "last_interaction")
    private LocalDateTime lastInteraction;
    
    @Column(name = "next_reset_date")
    private LocalDateTime nextResetDate;

    @Column(name = "conversation_state")
    private String conversationState;


    // Atributos Adicionales
    private String sede;

    private String carrera;

}
