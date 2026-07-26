package com.photobogota.api.model;

/**
 * Identifica QUÉ se está reportando, de forma estructurada, para que
 * el dashboard de la Etapa 2 pueda filtrar y mostrarlo sin tener que
 * interpretar texto libre.
 *
 * - SPOT: se reporta el spot en general (info incorrecta, error técnico,
 *   problema con el lugar). Si el spot fue creado por un SOCIO, además
 *   Reporte.esLocalDeSocio queda en true para que el dashboard lo etiquete
 *   como "Local" en vez de "Spot".
 * - RESENA: se reporta una reseña puntual dentro de un spot (Reporte.resenaId
 *   queda seteado).
 */
public enum TipoObjetivoReporte {
    SPOT,
    RESENA
}
