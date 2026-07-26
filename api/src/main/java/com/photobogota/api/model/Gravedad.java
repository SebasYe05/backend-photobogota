package com.photobogota.api.model;

/**
 * Nivel de gravedad de un reporte, usado para priorizar automáticamente
 * el dashboard de la Etapa 2 (los más críticos primero).
 * Se calcula en el service al crear el reporte (ver ReporteServiceImpl)
 * a partir de la categoría y de si hay reportes activos previos sobre
 * el mismo objetivo (reincidencia), y se puede elevar al escalar.
 */
public enum Gravedad {
    BAJA,
    MEDIA,
    ALTA,
    CRITICA
}
