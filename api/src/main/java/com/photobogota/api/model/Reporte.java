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

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "reportes")
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

    // Id de la reseña puntual dentro de Spot.resenas, solo cuando
    // tipoObjetivo = RESENA.
    private String resenaId;

    // nombreUsuario del autor de la reseña reportada, denormalizado para
    // mostrarlo directo en el dashboard sin tener que ir a buscar el spot.
    private String autorResenaReportada;

    // A quien se le asigna automaticamente el reporte segun la categoria (MOD o ADMIN)
    private Rol asignadoA;

    @Builder.Default
    private EstadoReporte estado = EstadoReporte.NUEVO;

    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    private LocalDateTime fechaActualizacion;
}
