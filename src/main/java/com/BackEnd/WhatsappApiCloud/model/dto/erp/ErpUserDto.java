package com.BackEnd.WhatsappApiCloud.model.dto.erp;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErpUserDto {
    private String codigoErp;
    private String tipoIdentificacion;
    private String identificacion;
    private String nombres;
    private String apellidos;
    private String numeroCelular;
    private String emailInstitucional;
    private String emailPersonal;
    private String sexo;
    private List<RolUserDto> rolesUsuario;
}
