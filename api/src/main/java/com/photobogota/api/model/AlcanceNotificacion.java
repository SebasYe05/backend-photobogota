package com.photobogota.api.model;

/**
 * Define a quién llega una notificación enviada manualmente por Admin/Moderador.
 *
 * TODOS               -> A todos los usuarios registrados.
 * POR_ROL             -> A todos los usuarios que tengan alguno de los roles indicados.
 * USUARIOS_ESPECIFICOS -> Solo a la lista de nombresUsuario indicados.
 */
public enum AlcanceNotificacion {
    TODOS,
    POR_ROL,
    USUARIOS_ESPECIFICOS
}
