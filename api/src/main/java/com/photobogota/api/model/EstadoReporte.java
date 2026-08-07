package com.photobogota.api.model;

public enum EstadoReporte {
    NUEVO,
    EN_REVISION,
    // Un SOCIO o un ADMIN marcó el reporte como solucionado, pero todavía
    // no lo valida un MOD. Solo al validarse pasa a RESUELTO y se notifica
    // al miembro afectado (HU 15 punto 4-5, HU 16 punto 4-5).
    PENDIENTE_VALIDACION,
    RESUELTO,
    RECHAZADO
}
