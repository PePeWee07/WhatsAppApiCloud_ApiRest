package com.BackEnd.WhatsappApiCloud.model.entity.OpenIA;

public record QuestionOpenIa(String pregunta, String nombre, String thread_id) {
    public QuestionOpenIa(String pregunta, String nombre) {
        this(pregunta, nombre, null);
    }
}
