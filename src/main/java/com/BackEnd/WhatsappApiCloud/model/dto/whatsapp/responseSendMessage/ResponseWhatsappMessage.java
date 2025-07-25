package com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.responseSendMessage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

//* Usado por --> ResponseWhatsapp.java
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResponseWhatsappMessage(String id) {
} 
