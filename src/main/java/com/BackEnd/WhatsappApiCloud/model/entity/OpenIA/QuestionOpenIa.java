package com.BackEnd.WhatsappApiCloud.model.entity.OpenIA;

public record QuestionOpenIa(String ask, String name, String thread_id) {
    public QuestionOpenIa(String ask, String name) {
        this(ask, name, null);
    }
}
