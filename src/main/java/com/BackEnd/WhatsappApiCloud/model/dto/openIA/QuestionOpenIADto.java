package com.BackEnd.WhatsappApiCloud.model.dto.openIA;

public record QuestionOpenIADto(String ask, String name, String phone, String rol, String thread_id) {
    public QuestionOpenIADto(String ask, String name, String phone, String rol) {
        this(ask, name, phone, rol, null);
    }
}
