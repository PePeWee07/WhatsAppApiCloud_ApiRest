package com.BackEnd.WhatsappApiCloud.model.dto.openIA;

import java.util.List;

public record QuestionOpenIADto(
        String ask,
        String name,
        String phone,
        List<String> roles,
        String previousResponseId,
        String identificacion,
        String emailInstitucional,
        String emailPersonal,
        String sexo) {
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
        this(ask, name, phone, roles, null, identifiacion, emailInstitucional, emailPersonal, sexo);
    }
}