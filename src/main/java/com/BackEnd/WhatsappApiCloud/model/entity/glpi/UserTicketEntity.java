package com.BackEnd.WhatsappApiCloud.model.entity.glpi;

import com.BackEnd.WhatsappApiCloud.model.entity.user.UserChatEntity;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Getter
@Setter
@Data
@Entity
@Table(name = "ticket")
@NoArgsConstructor
@AllArgsConstructor
public class UserTicketEntity {

    @Id
    private Long id;

    @Column(name = "whatsapp_phone", nullable = false)
    private String whatsappPhone;

    private String name;
    private String status;

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
