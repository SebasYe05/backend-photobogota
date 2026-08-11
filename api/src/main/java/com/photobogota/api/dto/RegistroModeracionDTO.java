package com.photobogota.api.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.photobogota.api.model.AccionModeracion;
import com.photobogota.api.model.EstadoApelacion;
import com.photobogota.api.model.TipoContenidoModerado;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO de un registro del historial de moderación (acciones del filtro automático)")
public class RegistroModeracionDTO {

    @Schema(description = "ID del registro")
    private String id;

    @Schema(description = "ID del usuario sancionado")
    private String usuarioId;

    @Schema(description = "nombreUsuario del usuario sancionado")
    private String nombreUsuario;

    @Schema(description = "Acción registrada")
    private AccionModeracion accion;

    @Schema(description = "Tipo de contenido donde se detectó la infracción")
    private TipoContenidoModerado tipoContenido;

    @Schema(description = "Contenido original que disparó la detección")
    private String contenidoOriginal;

    @Schema(description = "Palabras o frases prohibidas detectadas")
    private List<String> palabrasDetectadas;

    @Schema(description = "Detalle/motivo de la acción")
    private String detalle;

    @Schema(description = "Origen de la acción: AUTO o nombreUsuario del admin")
    private String origen;

    @Schema(description = "Fecha de la acción")
    private LocalDateTime fecha;

    @Schema(description = "Estado de la apelación (solo aplica a acciones BAN)")
    private EstadoApelacion estadoApelacion;

    @Schema(description = "Motivo escrito por el usuario al apelar")
    private String motivoApelacion;

    @Schema(description = "Fecha en que el usuario apeló")
    private LocalDateTime fechaApelacion;

    @Schema(description = "Respuesta del admin a la apelación")
    private String respuestaApelacion;

    @Schema(description = "nombreUsuario del admin que resolvió la apelación")
    private String revisadaPor;

    @Schema(description = "Fecha en que se resolvió la apelación")
    private LocalDateTime fechaRevision;
}
