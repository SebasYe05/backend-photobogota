package com.photobogota.api.dto;

import com.photobogota.api.model.Calificacion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Data
@Schema(description = "DTO de respuesta de una calificacion (reseña)")
public class CalificacionResponseDTO {

    @Schema(description = "ID de la calificacion")
    private String id;

    @Schema(description = "ID del spot calificado")
    private String spotId;

    @Schema(description = "Nombre del spot calificado", example = "Parque Simón Bolívar")
    private String tituloSpot;

    @Schema(description = "Usuario que realizo la calificacion")
    private String usuario;

    @Schema(description = "Calificación en estrellas (contrato del front)", example = "5")
    private Integer rating;

    @Schema(description = "Comentario del usuario (contrato del front)")
    private String texto;

    @Schema(description = "Likes de la reseña (0 si el concepto no existe aún)", example = "0")
    private Integer likes = 0;

    @Schema(description = "Fecha de creación en ISO-8601 UTC", example = "2026-06-01T15:30:00Z")
    private String fechaCreacion;

    // --- Campos legacy: se conservan para no romper otros clientes que ya los consuman ---

    @Schema(description = "[legacy] Calificacion en estrellas", example = "5")
    private Integer estrellas;

    @Schema(description = "[legacy] Comentario de la calificacion")
    private String comentario;

    @Schema(description = "[legacy] Fecha de la calificacion en formato legible", example = "Hace 2 dias")
    private String fecha;

    public static CalificacionResponseDTO from(Calificacion calificacion) {
        return from(calificacion, null);
    }

    public static CalificacionResponseDTO from(Calificacion calificacion, String tituloSpot) {
        CalificacionResponseDTO dto = new CalificacionResponseDTO();
        dto.setId(calificacion.getId());
        dto.setSpotId(calificacion.getSpotId());
        dto.setTituloSpot(tituloSpot);
        dto.setUsuario(calificacion.getUsuario());
        dto.setRating(calificacion.getEstrellas());
        dto.setTexto(calificacion.getComentario());
        dto.setLikes(0);
        dto.setFechaCreacion(formatearFechaISO(calificacion.getFecha()));

        // legacy
        dto.setEstrellas(calificacion.getEstrellas());
        dto.setComentario(calificacion.getComentario());
        dto.setFecha(formatearFecha(calificacion.getFecha()));
        return dto;
    }

    private static String formatearFechaISO(LocalDateTime fecha) {
        if (fecha == null) {
            return null;
        }
        return fecha.toInstant(ZoneOffset.UTC).toString();
    }

    private static String formatearFecha(LocalDateTime fecha) {
        if (fecha == null)
            return "Recientemente";
        long dias = ChronoUnit.DAYS.between(fecha.toLocalDate(), LocalDateTime.now().toLocalDate());
        if (dias == 0)
            return "Hoy";
        if (dias == 1)
            return "Ayer";
        if (dias < 7)
            return "Hace " + dias + " dias";
        if (dias < 30)
            return "Hace " + (dias / 7) + " semanas";
        if (dias < 365)
            return "Hace " + (dias / 30) + " meses";
        return "Hace " + (dias / 365) + " anos";
    }
}
