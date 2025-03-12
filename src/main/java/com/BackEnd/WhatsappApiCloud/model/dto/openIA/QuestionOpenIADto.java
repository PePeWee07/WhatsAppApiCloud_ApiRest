package com.BackEnd.WhatsappApiCloud.model.dto.openIA;

public record QuestionOpenIADto(String ask, String name, String phone, String thread_id) {
    public QuestionOpenIADto(String ask, String name, String phone) {
        this(ask, name, phone, null);
    }
}
