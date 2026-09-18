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

    // nombreUsuario del SOCIO dueño del spot reportado, denormalizado desde
    // Spot.creadorUsername al crear el reporte. Solo se llena cuando
    // esLocalDeSocio = true. Sirve para filtrar el dashboard del socio a
    // "solo lo mío" sin tener que ir a buscar el spot en cada consulta.
    private String propietarioSocio;

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

    // true si el reporte fue escalado al menos una vez (SOCIO->MOD o
    // MOD->ADMIN). Con el módulo unificado (HU 24) un reporte puede pasar
    // por más de un nivel; estos 4 campos siempre reflejan el escalamiento
    // MÁS RECIENTE, y el detalle completo queda en "historialEscalamiento".
    @Builder.Default
    private Boolean escalado = false;

    private LocalDateTime fechaEscalado;

    // nombreUsuario de quien escaló el reporte (SOCIO, MOD, o "sistema" si
    // fue un escalamiento automático por vencimiento de plazo).
    private String escaladoPor;

    private String motivoEscalado;

    // true si el último escalamiento fue automático (SLA vencido, HU 15 pt 7)
    // y no una decisión manual de un SOCIO/MOD.
    @Builder.Default
    private Boolean escaladoAutomaticamente = false;

    // Bitácora de TODOS los escalamientos del reporte, en orden cronológico
    // (HU 24: escalamiento entre SOCIO, MOD y ADMIN, con posibilidad de
    // re-escalar y de documentar lo actuado en cada salto de nivel).
    @Builder.Default
    private List<Escalamiento> historialEscalamiento = new ArrayList<>();

    // nombreUsuario de quien marcó el reporte como "Solucionado" cuando
    // quien lo resuelve es un SOCIO o un ADMIN (queda pendiente de que un
    // MOD lo valide antes de notificar al miembro; ver estado
    // PENDIENTE_VALIDACION).
    private String resueltoPor;

    // nombreUsuario del MOD que validó (o rechazó la validación de) la
    // solución propuesta por un SOCIO/ADMIN.
    private String validadoPor;

    private LocalDateTime fechaValidacion;

    // Plazos de atención (HU 15 punto 6), solo aplican cuando el reporte
    // está asignado a un SOCIO: debe responder (salir de NUEVO) en máximo
    // 24h, y resolverlo en máximo 5 días. Si vence el plazo de respuesta,
    // el scheduler de escalamiento automático lo sube a MOD.
    private LocalDateTime fechaLimiteRespuesta;

    private LocalDateTime fechaLimiteResolucion;

    // nombreUsuario de quien hizo el último cambio de estado (auditoría)
    private String actualizadoPor;

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

    // Un salto de nivel dentro de la cadena SOCIO -> MOD -> ADMIN (HU 24).
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Escalamiento {
        private Rol de;
        private Rol a;
        private String por;
        private String motivo;
        private Boolean automatico;
        private LocalDateTime fecha;
    }
}
