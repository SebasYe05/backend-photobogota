package com.photobogota.api.model;

public enum EstadoAspirante {
    // Solicitud recién creada o reenviada tras una corrección, esperando revisión.
    PENDIENTE,
    // El moderador la devolvió al aspirante para que corrija datos/documentos.
    EN_CORRECCION,
    // Aprobada por el moderador. Queda a la espera de que el módulo de
    // creación de cuentas genere el rol de SOCIO y envíe las credenciales.
    ENVIO_CREDENCIALES,
    // Proceso completo: ya se le creó la cuenta y se le enviaron las credenciales.
    APROBADO,
    RECHAZADO
}
