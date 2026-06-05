package com.BackEnd.WhatsappApiCloud.model.dto.openIA;

import java.util.List;
import java.util.Map;

public record QuestionOpenIADto(
        String ask,
        String name,
        String phone,
        List<String> roles,
        String previousResponseId,
        String identificacion,
        String emailInstitucional,
        String emailPersonal,
        String sexo,
        Map<String, List<String>> toolPermissions,
        Map<String, Integer> toolCooldowns,
        Map<String, Integer> toolCooldownRemaining) {
    public QuestionOpenIADto(
            String ask,
            String name,
            String phone,
            List<String> roles,
            String identifiacion,
            String emailInstitucional,
            String emailPersonal,
            String sexo
        ) {
        this(ask, name, phone, roles, null, identifiacion, emailInstitucional, emailPersonal, sexo, null, null, null);
    }
}