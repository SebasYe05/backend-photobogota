package com.photobogota.api.exception;

import java.time.LocalDateTime;
import java.util.List;

import com.photobogota.api.model.TipoSancion;

import lombok.Getter;

/**
 * Se lanza cuando el filtro automático de contenido detecta una infracción o
 * cuando un usuario con sanción activa intenta publicar contenido.
 */
@Getter
public class ContenidoInapropiadoException extends RuntimeException {

    private final List<String> palabrasDetectadas;
    private final TipoSancion sancionAplicada;
    private final LocalDateTime fechaExpiracionSancion;
    private final Integer contadorInfracciones;

    public ContenidoInapropiadoException(String message, List<String> palabrasDetectadas,
            TipoSancion sancionAplicada, LocalDateTime fechaExpiracionSancion, Integer contadorInfracciones) {
        super(message);
        this.palabrasDetectadas = palabrasDetectadas;
        this.sancionAplicada = sancionAplicada;
        this.fechaExpiracionSancion = fechaExpiracionSancion;
        this.contadorInfracciones = contadorInfracciones;
    }
}
