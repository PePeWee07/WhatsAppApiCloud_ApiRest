package com.BackEnd.WhatsappApiCloud.model.entity.whatsapp;

//* number --> Numero de telefono al que se le enviara el mensaje
//* message --> Mensaje que se enviara
//* sentBy --> Quien respondio el mensaje = profileName (si es back-office poner nombre del usuario)
//* source --> como se envia el mensaje = source (back-end, back-office, IA)
//* businessPhoneNumber --> Numero de telefono del negocio que envia el mensaje
//* type --> Tipo de mensaje (text, image, document, audio, video, sticker, etc)
//* contextId --> Reply de mensaje = relatedMessageId

public record MessageBody(
    String number,
    String message,
    String sentBy,
    String source,
    String businessPhoneNumber,
    String type,
    String contextId
) {
    public MessageBody(String number, String message, String sentBy, String businessPhoneNumber, String type, String source) {
        this(number, message, sentBy, source, businessPhoneNumber, type, null);
    }
}
