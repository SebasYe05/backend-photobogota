package com.photobogota.api.model;

/**
 * Motivos predefinidos que un miembro puede indicar al solicitar la
 * eliminación de su cuenta. Se mantienen como enum (en vez de texto libre)
 * para poder generar métricas agrupadas por motivo más adelante.
 */
public enum MotivoEliminacionCuenta {
    NO_USO_LA_APLICACION,
    ENCONTRE_OTRA_ALTERNATIVA,
    PREOCUPACIONES_DE_PRIVACIDAD,
    MALA_EXPERIENCIA_DE_USO,
    DEMASIADAS_NOTIFICACIONES,
    OTRO
}
