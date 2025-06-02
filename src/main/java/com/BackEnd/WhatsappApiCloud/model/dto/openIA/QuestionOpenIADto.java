package com.BackEnd.WhatsappApiCloud.model.dto.openIA;

import java.util.List;

public record QuestionOpenIADto(
        String ask,
        String name,
        String phone,
        List<String> userRoles,
        String thread_id) {
    public QuestionOpenIADto(String ask, String name, String phone, List<String> userRoles) {
        this(ask, name, phone, userRoles, null);
    }
}