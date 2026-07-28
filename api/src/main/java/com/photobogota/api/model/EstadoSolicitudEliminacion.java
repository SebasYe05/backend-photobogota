package com.photobogota.api.model;

/**
 * Estados por los que pasa una solicitud de eliminación de cuenta.
 *
 * PENDIENTE_VERIFICACION -> se generó el código y se envió por correo, pero
 *                            el usuario todavía no lo confirmó.
 * PROGRAMADA             -> el usuario confirmó con el código; la cuenta
 *                            quedará anonimizada automáticamente cuando se
 *                            cumpla fechaProgramadaEliminacion (30 días).
 * CANCELADA              -> el usuario recuperó su cuenta antes de esa fecha.
 * COMPLETADA             -> se cumplió el plazo y el job programado anonimizó
 *                            los datos del usuario.
 */
public enum EstadoSolicitudEliminacion {
    PENDIENTE_VERIFICACION,
    PROGRAMADA,
    CANCELADA,
    COMPLETADA
}
