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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 1, max = 100, message = "El nombre de usuario debe tener entre 1 y 100 caracteres")
    @Column(name = "name", length = 100, nullable = false)
    private String nombres;

    private String cedula;

    private String phone;

    private String rol;

    private String sede;

    private String carrera;

    private String thread_id;

    private int limite = 10;

    private LocalDateTime firstInteraction;
    
    private LocalDateTime lastInteraction;
    
    private LocalDateTime nextResetDate;

    private String conversationState;

}
