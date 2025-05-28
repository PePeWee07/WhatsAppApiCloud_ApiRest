package com.BackEnd.WhatsappApiCloud.model.dto.erp;

import lombok.Data;
import java.util.List;

@Data
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
