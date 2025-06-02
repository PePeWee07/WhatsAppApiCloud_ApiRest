package com.BackEnd.WhatsappApiCloud.model.entity.user;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "erp_role_detail")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString
public class ErpRoleDetailEntity {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Puede haber campos que solo apliquen a estudiante
    @Column(name = "id_carrera")
    private String idCarrera;

    @Column(name = "nombre_carrera")
    private String nombreCarrera;

    @Column(name = "ultimo_semestre_activo")
    private String ultimoSemestreActivo;

    @Column(name = "unidad_academica")
    private String unidadAcademica;

    @Column(name = "sede")
    private String sede;

    @Column(name = "modalidad")
    private String modalidad;

    @Column(name = "curso")
    private String curso;

    @Column(name = "paralelo")
    private String paralelo;

    // Campos que aplican para roles de personal
    @Column(name = "nombre_rol", length = 100)
    private String nombreRol;

    @Column(name = "unidad_organizativa", length = 150)
    private String unidadOrganizativa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "erp_role_id",
        referencedColumnName = "id",
        nullable = false
    )
    @JsonBackReference
    private ErpRoleEntity role;
}

