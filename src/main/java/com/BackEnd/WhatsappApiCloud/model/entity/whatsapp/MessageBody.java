package com.BackEnd.WhatsappApiCloud.model.entity.whatsapp;

//* number --> Numero de telefono al que se le enviara el mensaje
//* message --> Mensaje que se enviara

public record MessageBody(String number, String message) {}
