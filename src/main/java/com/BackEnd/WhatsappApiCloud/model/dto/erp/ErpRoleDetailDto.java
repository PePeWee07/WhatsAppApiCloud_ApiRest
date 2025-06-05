package com.BackEnd.WhatsappApiCloud.model.dto.erp;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErpRoleDetailDto implements Serializable {
    
    // Campos para roles de estudiante
    private String idCarrera;
    private String nombreCarrera;
    private String ultimoSemestreActivo;
    private String unidadAcademica;
    private String sede;
    private String modalidad;
    private String curso;
    private String paralelo;

    // Campos para roles de personal
    private String nombreRol;
    private String unidadOrganizativa;
}
