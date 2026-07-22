package com.photobogota.api.dto;

import com.photobogota.api.model.Calificacion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
@Schema(description = "DTO de respuesta de una calificacion")
public class CalificacionResponseDTO {

    @Schema(description = "ID de la calificacion")
    private String id;

    @Schema(description = "ID del spot calificado")
    private String spotId;

    @Schema(description = "Usuario que realizo la calificacion")
    private String usuario;

    @Schema(description = "Calificacion en estrellas", example = "5")
    private Integer estrellas;

    @Schema(description = "Comentario de la calificacion")
    private String comentario;

    @Schema(description = "Fecha de la calificacion en formato legible", example = "Hace 2 dias")
    private String fecha;

    public static CalificacionResponseDTO from(Calificacion calificacion) {
        CalificacionResponseDTO dto = new CalificacionResponseDTO();
        dto.setId(calificacion.getId());
        dto.setSpotId(calificacion.getSpotId());
        dto.setUsuario(calificacion.getUsuario());
        dto.setEstrellas(calificacion.getEstrellas());
        dto.setComentario(calificacion.getComentario());
        dto.setFecha(formatearFecha(calificacion.getFecha()));
        return dto;
    }

    private static String formatearFecha(LocalDateTime fecha) {
        if (fecha == null) return "Recientemente";
        long dias = ChronoUnit.DAYS.between(fecha, LocalDateTime.now());
        if (dias == 0)   return "Hoy";
        if (dias == 1)   return "Ayer";
        if (dias < 7)    return "Hace " + dias + " dias";
        if (dias < 30)   return "Hace " + (dias / 7) + " semanas";
        if (dias < 365)  return "Hace " + (dias / 30) + " meses";
        return "Hace " + (dias / 365) + " anos";
    }
}
