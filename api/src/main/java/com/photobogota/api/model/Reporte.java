package com.photobogota.api.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "reportes")
@FieldNameConstants
public class Reporte {

    @Id
    private String id;

    // Ticket legible para el usuario, ej: "REP-482913"
    @Indexed(unique = true)
    private String numeroTicket;

    private CategoriaReporte categoria;

    private String descripcion;

    // URLs de capturas de pantalla ya subidas (via /api/v1/imagenes/reporte)
    @Builder.Default
    private List<String> evidencias = new ArrayList<>();

    // nombreUsuario de quien crea el reporte
    private String reportadoPor;

    // Que se está reportando: el spot/local en general, o una reseña puntual.
    // Se calcula automáticamente en el service: si viene resenaId -> RESENA, si no -> SPOT.
    private TipoObjetivoReporte tipoObjetivo;

    // Opcional: si el reporte esta asociado a un spot puntual
    private String spotId;

    // Denormalizado desde Spot al momento de crear el reporte, para que el
    // dashboard no tenga que hacer un join solo para mostrar el nombre.
    private String nombreSpot;

    // true si el spot reportado fue creado por un SOCIO (osea, es "un local"
    // y no un spot cargado por un moderador/administrador). Ayuda a que el
    // dashboard de la Etapa 2 distinga "Spot" de "Local" de forma clara.
    private Boolean esLocalDeSocio;

    // Id de la reseña reportada dentro de la colección "calificaciones",
    // solo cuando tipoObjetivo = RESENA.
    private String resenaId;

    // nombreUsuario del autor de la reseña reportada, denormalizado para
    // mostrarlo directo en el dashboard sin tener que ir a buscar el spot.
    private String autorResenaReportada;

    // A quien se le asigna automaticamente el reporte segun la categoria (MOD o ADMIN).
    // Al escalar, cambia de MOD a ADMIN.
    private Rol asignadoA;

    // Gravedad calculada automáticamente al crear el reporte (Etapa 2, punto 6):
    // según la categoría y si hay reportes activos previos sobre el mismo
    // objetivo (reincidencia). Se usa para priorizar el dashboard.
    private Gravedad gravedad;

    // true si un moderador escaló el reporte a un administrador
    @Builder.Default
    private Boolean escalado = false;

    private LocalDateTime fechaEscalado;

    // nombreUsuario del moderador que escaló el reporte
    private String escaladoPor;

    private String motivoEscalado;

    // nombreUsuario de quien hizo el último cambio de estado (auditoría)
    private String actualizadoPor;

    // nombreUsuario (SOCIO/ADMIN) que propuso la solución. Cuando un SOCIO o un
    // ADMIN marca un reporte como RESUELTO, el backend lo deja en
    // PENDIENTE_VALIDACION (con este campo cargado) hasta que un MOD lo apruebe.
    private String resueltoPor;

    // Bitácora de observaciones dejadas por MOD/ADMIN al cambiar el estado.
    // Estructurado (no se mezcla con la descripción original del reportante).
    @Builder.Default
    private List<Observacion> bitacora = new ArrayList<>();

    @Builder.Default
    private EstadoReporte estado = EstadoReporte.NUEVO;

    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    private LocalDateTime fechaActualizacion;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Observacion {
        private String autor;
        private String texto;
        private LocalDateTime fecha;
    }
}
