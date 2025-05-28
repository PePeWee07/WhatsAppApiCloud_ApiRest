package com.BackEnd.WhatsappApiCloud.model.dto.erp;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.List;
import java.util.Map;

@Data
public class RolUserDto {
    private String tipoRol;

    /**
     * Permite que Jackson trate un objeto único como lista de un solo elemento.
     * Cuando reciba un valor que no es un array {...}, lo envuelva automáticamente [{...}]
     */
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<Map<String, Object>> detallesRol;
}
