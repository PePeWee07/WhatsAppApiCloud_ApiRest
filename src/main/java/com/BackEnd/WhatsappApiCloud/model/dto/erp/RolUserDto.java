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
public class RolUserDto {
    private String tipoRol;
    private List<ErpRoleDetailDto> detallesRol;
}
