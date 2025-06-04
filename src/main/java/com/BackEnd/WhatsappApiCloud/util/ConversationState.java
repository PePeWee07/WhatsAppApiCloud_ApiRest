package com.BackEnd.WhatsappApiCloud.util;

/**
 * Enum que representa los posibles estados de una conversación con el usuario.
 * 
 * - NEW: Primer contacto, aún no se ha interactuado.
 * - ASKED_FOR_CEDULA: Se ha preguntado por la cédula del usuario.
 * - READY: Listo para interactuar con el usuario.
 */

public enum ConversationState {
    NEW,                  
    ASKED_FOR_CEDULA,     
    READY                 
}
