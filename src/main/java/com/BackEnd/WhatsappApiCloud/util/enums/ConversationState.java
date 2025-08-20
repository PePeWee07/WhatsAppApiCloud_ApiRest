package com.BackEnd.WhatsappApiCloud.util.enums;

/**
 * Enum que representa los posibles estados de una conversación con el usuario.
 * 
 * - NEW: Primer contacto, aún no se ha interactuado.
 * - ASKED_FOR_CEDULA: Se ha preguntado por la cédula del usuario.
 * - READY: Listo para interactuar con el usuario.
 * - WAITING_SUBJECTS: Esperando a que el usuario envíe los temas de interés.
 */

public enum ConversationState {
    NEW,                  
    ASKED_FOR_CEDULA,     
    READY,
    WAITING_ATTACHMENTS,
    WAITING_ATTACHMENTS_FOR_TICKET_EXISTING
}