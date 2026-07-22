package com.photobogota.api.model;

/**
 * Tipos de notificación que puede recibir un usuario.
 *
 * NUEVO_SPOT_INTERES  -> Se publicó un spot en una localidad/categoría de interés del usuario.
 * NUEVA_RESENA        -> Alguien dejó una reseña en un spot que el usuario publicó.
 * ANUNCIO_ADMIN       -> Anuncio manual enviado por un administrador.
 * ANUNCIO_MODERADOR   -> Anuncio manual enviado por un moderador.
 * SISTEMA             -> Notificaciones generales del sistema (uso genérico/futuro).
 */
public enum NotificacionTipo {
    NUEVO_SPOT_INTERES,
    NUEVA_RESENA,
    ANUNCIO_ADMIN,
    ANUNCIO_MODERADOR,
    SISTEMA
}
