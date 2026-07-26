package com.photobogota.api.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.photobogota.api.model.CategoriaReporte;
import com.photobogota.api.model.EstadoReporte;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.TipoObjetivoReporte;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO de respuesta para un reporte")
public class ReporteResponseDTO {

    @Schema(description = "ID del reporte")
    private String id;

    @Schema(description = "Numero de ticket generado automaticamente", example = "REP-482913")
    private String numeroTicket;

    @Schema(description = "Categoria del reporte")
    private CategoriaReporte categoria;

    @Schema(description = "Descripcion del reporte")
    private String descripcion;

    @Schema(description = "URLs de evidencia adjuntadas")
    private List<String> evidencias;

    @Schema(description = "nombreUsuario de quien creo el reporte")
    private String reportadoPor;

    @Schema(description = "Que se está reportando: SPOT (el lugar en general) o RESENA (una reseña puntual)")
    private TipoObjetivoReporte tipoObjetivo;

    @Schema(description = "ID del spot relacionado, si aplica")
    private String spotId;

    @Schema(description = "Nombre del spot relacionado, para mostrar directo en el dashboard")
    private String nombreSpot;

    @Schema(description = "true si el spot reportado es un local creado por un SOCIO (no un spot de moderador/admin)")
    private Boolean esLocalDeSocio;

    @Schema(description = "ID de la reseña reportada, solo si tipoObjetivo = RESENA")
    private String resenaId;

    @Schema(description = "nombreUsuario del autor de la reseña reportada, solo si tipoObjetivo = RESENA")
    private String autorResenaReportada;

    @Schema(description = "Rol al que fue asignado automaticamente el reporte (MOD o ADMIN)")
    private Rol asignadoA;

    @Schema(description = "Estado actual del reporte")
    private EstadoReporte estado;

    @Schema(description = "Fecha de creacion del reporte")
    private LocalDateTime fechaCreacion;

    @Schema(description = "Fecha de la ultima actualizacion del reporte")
    private LocalDateTime fechaActualizacion;
}
