package com.BackEnd.WhatsappApiCloud.model.entity.user;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "erp_role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ErpRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_rol", length = 50, nullable = false)
    private String tipoRol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "user_chat_id", 
        referencedColumnName = "id", 
        insertable = false, 
        nullable = false
    )
    @JsonBackReference
    private UserChatEntity user;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    @JsonManagedReference
    private Set<ErpRoleDetailEntity> detallesRol = new HashSet<>();
}
