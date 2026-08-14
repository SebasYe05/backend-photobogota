package com.photobogota.api.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.photobogota.api.dto.CambiarEstadoRequestDTO;
import com.photobogota.api.dto.CrearReporteRequestDTO;
import com.photobogota.api.dto.EscalarReporteRequestDTO;
import com.photobogota.api.dto.ReporteResponseDTO;
import com.photobogota.api.dto.ValidarReporteRequestDTO;
import com.photobogota.api.exception.AccessForbiddenException;
import com.photobogota.api.exception.OperacionInvalidaException;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.model.Calificacion;
import com.photobogota.api.model.CategoriaReporte;
import com.photobogota.api.model.EstadoReporte;
import com.photobogota.api.model.Gravedad;
import com.photobogota.api.model.Reporte;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.Spot;
import com.photobogota.api.model.TipoObjetivoReporte;
import com.photobogota.api.repository.CalificacionRepository;
import com.photobogota.api.repository.ReporteRepository;
import com.photobogota.api.repository.SpotRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements IReporteService {

    // Reportes en estos estados cuentan como "activos" para calcular reincidencia
    private static final List<EstadoReporte> ESTADOS_ACTIVOS = List.of(EstadoReporte.NUEVO, EstadoReporte.EN_REVISION);

    private final ReporteRepository reporteRepository;
    private final SpotRepository spotRepository;
    private final CalificacionRepository calificacionRepository;
    private final MongoTemplate mongoTemplate;
    private final IPuntosService puntosService;
    private final INotificacionService notificacionService;

    @Override
    public ReporteResponseDTO crearReporte(CrearReporteRequestDTO request, String usuario) {

        Spot spot = null;
        Calificacion calificacionReportada = null;

        // Si viene spotId, lo resolvemos una sola vez para sacar nombre y si es
        // un local de socio. Las reseñas NO viven embebidas en Spot: se guardan
        // en la colección aparte "calificaciones" (ver CalificacionRepository),
        // así que resenaId se busca ahí, no dentro de spot.getResenas().
        if (request.getSpotId() != null && !request.getSpotId().isBlank()) {
            spot = spotRepository.findById(request.getSpotId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Spot no encontrado con id: " + request.getSpotId()));

            if (request.getResenaId() != null && !request.getResenaId().isBlank()) {
                calificacionReportada = calificacionRepository.findById(request.getResenaId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Reseña no encontrada con id: " + request.getResenaId()));

                if (!request.getSpotId().equals(calificacionReportada.getSpotId())) {
                    throw new ResourceNotFoundException(
                            "La reseña " + request.getResenaId() + " no pertenece al spot " + request.getSpotId());
                }

                // Un miembro no puede reportar su propia reseña.
                if (usuario != null && usuario.equals(calificacionReportada.getUsuario())) {
                    throw new AccessForbiddenException("No puedes reportar tu propia reseña");
                }
            }
        } else if (request.getResenaId() != null && !request.getResenaId().isBlank()) {
            // No debería pasar desde el front (siempre se manda spotId junto con
            // resenaId), pero lo validamos por si llega una petición directa a la API.
            throw new ResourceNotFoundException(
                    "Debes indicar el spotId al que pertenece la reseña reportada");
        }

        TipoObjetivoReporte tipoObjetivo = calificacionReportada != null
                ? TipoObjetivoReporte.RESENA
                : TipoObjetivoReporte.SPOT;

        long reportesActivosPrevios = contarReportesActivosPrevios(request.getSpotId(), request.getResenaId());
        Gravedad gravedad = calcularGravedad(request.getCategoria(), reportesActivosPrevios);

        Reporte reporte = Reporte.builder()
                .numeroTicket(generarNumeroTicketUnico())
                .categoria(request.getCategoria())
                .descripcion(request.getDescripcion())
                .evidencias(request.getEvidencias() != null ? request.getEvidencias() : List.of())
                .reportadoPor(usuario)
                .tipoObjetivo(tipoObjetivo)
                .spotId(request.getSpotId())
                .nombreSpot(spot != null ? spot.getNombre() : null)
                .esLocalDeSocio(spot != null ? "SOCIO".equals(spot.getCreadorRol()) : null)
                .resenaId(calificacionReportada != null ? calificacionReportada.getId() : null)
                .autorResenaReportada(calificacionReportada != null ? calificacionReportada.getUsuario() : null)
                .asignadoA(asignarResponsable(request.getCategoria()))
                .gravedad(gravedad)
                .estado(EstadoReporte.NUEVO)
                .fechaCreacion(LocalDateTime.now())
                .build();

        Reporte guardado = reporteRepository.save(reporte);
        return mapearADTO(guardado);
    }

    @Override
    public ReporteResponseDTO obtenerPorId(String id) {
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado con id: " + id));
        return mapearADTO(reporte);
    }

    @Override
    public List<ReporteResponseDTO> listarMisReportes(String usuario) {
        return reporteRepository.findByReportadoPor(usuario).stream()
                .map(this::mapearADTO)
                .toList();
    }

    @Override
    public List<ReporteResponseDTO> listarPorRolAsignado(Rol rol) {
        return reporteRepository.findByAsignadoA(rol).stream()
                .map(this::mapearADTO)
                .toList();
    }

    @Override
    public List<ReporteResponseDTO> obtenerDashboard(
            Rol rolUsuario,
            String username,
            EstadoReporte estado,
            Gravedad gravedad,
            CategoriaReporte categoria,
            TipoObjetivoReporte tipoObjetivo,
            Boolean escalado,
            String orden) {

        Query query = new Query();

        if (rolUsuario == Rol.SOCIO) {
            // Un SOCIO atiende los reportes sobre SUS propios locales.
            List<String> misSpotIds = spotRepository.findByCreadorUsername(username).stream()
                    .map(Spot::getId)
                    .toList();
            if (misSpotIds.isEmpty()) {
                return List.of();
            }
            query.addCriteria(Criteria.where(Reporte.Fields.spotId).in(misSpotIds));
        } else {
            // ADMIN solo ve lo que le fue asignado a ADMIN: los reportes de
            // categoría ERROR_TECNICO (asignación automática) y los que un
            // moderador escaló (escalar() cambia asignadoA a ADMIN). NO ve
            // la cola de MOD que todavía no fue escalada.
            // MOD solo ve su propia cola (lo que sigue asignado a MOD).
            query.addCriteria(Criteria.where(Reporte.Fields.asignadoA).is(rolUsuario));
        }

        if (estado != null) {
            query.addCriteria(Criteria.where(Reporte.Fields.estado).is(estado));
        }
        if (gravedad != null) {
            query.addCriteria(Criteria.where(Reporte.Fields.gravedad).is(gravedad));
        }
        if (categoria != null) {
            query.addCriteria(Criteria.where(Reporte.Fields.categoria).is(categoria));
        }
        if (tipoObjetivo != null) {
            query.addCriteria(Criteria.where(Reporte.Fields.tipoObjetivo).is(tipoObjetivo));
        }
        if (escalado != null) {
            query.addCriteria(Criteria.where(Reporte.Fields.escalado).is(escalado));
        }

        List<Reporte> reportes = mongoTemplate.find(query, Reporte.class);

        Comparator<Reporte> comparador = construirComparador(orden);

        return reportes.stream()
                .sorted(comparador)
                .map(this::mapearADTO)
                .toList();
    }

    @Override
    public ReporteResponseDTO cambiarEstado(String id, CambiarEstadoRequestDTO request, String usuario,
            Rol rolUsuario) {
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado con id: " + id));

        validarPropiedad(reporte, rolUsuario, usuario);

        // Si un SOCIO o un ADMIN marca como RESUELTO, la solución no se
        // notifica de inmediato: el reporte queda PENDIENTE_VALIDACION hasta
        // que un MOD la apruebe (HU 15 pt 4-5, HU 16 pt 4-5).
        EstadoReporte estadoFinal = request.getEstado();
        if (request.getEstado() == EstadoReporte.RESUELTO && rolUsuario != Rol.MOD) {
            estadoFinal = EstadoReporte.PENDIENTE_VALIDACION;
            reporte.setResueltoPor(usuario);
        } else {
            reporte.setResueltoPor(null);
        }

        reporte.setEstado(estadoFinal);
        reporte.setActualizadoPor(usuario);
        reporte.setFechaActualizacion(LocalDateTime.now());

        if (request.getObservacion() != null && !request.getObservacion().isBlank()) {
            if (reporte.getBitacora() == null) {
                reporte.setBitacora(new ArrayList<>());
            }
            reporte.getBitacora().add(Reporte.Observacion.builder()
                    .autor(usuario)
                    .texto(request.getObservacion())
                    .fecha(LocalDateTime.now())
                    .build());
        }

        Reporte actualizado = reporteRepository.save(reporte);

        // Solo un MOD que marca RESUELTO de inmediato otorga puntos (un
        // SOCIO/ADMIN pasa por PENDIENTE_VALIDACION y los puntos los otorga
        // validarReporte() cuando el MOD aprueba).
        if (request.getEstado() == EstadoReporte.RESUELTO && rolUsuario == Rol.MOD
                && reporte.getReportadoPor() != null) {
            otorgarPuntosPorValidacion(reporte);
        }

        return mapearADTO(actualizado);
    }

    @Override
    public ReporteResponseDTO escalarReporte(String id, EscalarReporteRequestDTO request, String usuario,
            Rol rolUsuario) {
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado con id: " + id));

        validarPropiedad(reporte, rolUsuario, usuario);

        if (Boolean.TRUE.equals(reporte.getEscalado())) {
            throw new OperacionInvalidaException("Este reporte ya fue escalado a un nivel superior");
        }

        // Cadena de escalamiento: SOCIO -> MOD -> ADMIN.
        if (rolUsuario == Rol.MOD) {
            reporte.setAsignadoA(Rol.ADMIN);
            // Un reporte escalado a administración pasa a ser prioritario.
            reporte.setGravedad(Gravedad.CRITICA);
        } else if (rolUsuario == Rol.SOCIO) {
            reporte.setAsignadoA(Rol.MOD);
        } else {
            throw new AccessForbiddenException("No tienes permiso para escalar este reporte");
        }

        reporte.setEscalado(true);
        reporte.setFechaEscalado(LocalDateTime.now());
        reporte.setEscaladoPor(usuario);
        reporte.setMotivoEscalado(request.getMotivo());
        reporte.setFechaActualizacion(LocalDateTime.now());

        Reporte actualizado = reporteRepository.save(reporte);
        return mapearADTO(actualizado);
    }

    @Override
    public List<ReporteResponseDTO> listarPendientesValidacion() {
        return reporteRepository.findByEstado(EstadoReporte.PENDIENTE_VALIDACION).stream()
                .map(this::mapearADTO)
                .toList();
    }

    @Override
    public ReporteResponseDTO validarReporte(String id, ValidarReporteRequestDTO request, String usuario) {
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado con id: " + id));

        if (reporte.getEstado() != EstadoReporte.PENDIENTE_VALIDACION) {
            throw new OperacionInvalidaException("Este reporte no está pendiente de validación");
        }

        if (request.getObservacion() != null && !request.getObservacion().isBlank()) {
            if (reporte.getBitacora() == null) {
                reporte.setBitacora(new ArrayList<>());
            }
            reporte.getBitacora().add(Reporte.Observacion.builder()
                    .autor(usuario)
                    .texto(request.getObservacion())
                    .fecha(LocalDateTime.now())
                    .build());
        }

        if (Boolean.TRUE.equals(request.getAprobado())) {
            reporte.setEstado(EstadoReporte.RESUELTO);
        } else {
            // Rechazada: vuelve a la cola de quien la propuso para que la revise.
            reporte.setEstado(EstadoReporte.EN_REVISION);
            reporte.setResueltoPor(null);
        }

        reporte.setActualizadoPor(usuario);
        reporte.setFechaActualizacion(LocalDateTime.now());

        Reporte actualizado = reporteRepository.save(reporte);

        if (Boolean.TRUE.equals(request.getAprobado()) && reporte.getReportadoPor() != null) {
            try {
                notificacionService.notificarSistema(reporte.getReportadoPor(),
                        "Tu reporte fue resuelto",
                        "El reporte " + reporte.getNumeroTicket()
                                + " fue validado por un moderador y quedó marcado como resuelto.");
            } catch (Exception e) {
                log.error("No se pudo notificar la resolución del reporte {}: {}", id, e.getMessage());
            }
            otorgarPuntosPorValidacion(reporte);
        }

        return mapearADTO(actualizado);
    }

    // Verifica que un MOD solo actúe sobre reportes que le pertenecen a su cola,
    // y que un SOCIO solo actúe sobre reportes de sus propios locales.
    // ADMIN tiene permiso sobre cualquier reporte (oversight).
    private void validarPropiedad(Reporte reporte, Rol rolUsuario, String usuario) {
        if (rolUsuario == Rol.MOD && reporte.getAsignadoA() != Rol.MOD) {
            throw new AccessForbiddenException("Este reporte no está asignado a moderación");
        }
        if (rolUsuario == Rol.SOCIO && !esLocalDelSocio(reporte, usuario)) {
            throw new AccessForbiddenException("Este reporte no pertenece a uno de tus locales");
        }
    }

    private boolean esLocalDelSocio(Reporte reporte, String usuario) {
        if (reporte.getSpotId() == null) {
            return false;
        }
        return spotRepository.findById(reporte.getSpotId())
                .map(spot -> usuario != null && usuario.equalsIgnoreCase(spot.getCreadorUsername()))
                .orElse(false);
    }

    private void otorgarPuntosPorValidacion(Reporte reporte) {
        try {
            puntosService.sumarPuntos(reporte.getReportadoPor(),
                    com.photobogota.api.model.TipoPuntos.REPORTE_VALIDADO, reporte.getId());
        } catch (Exception e) {
            log.error("No se pudo otorgar puntos por reporte validado {}: {}", reporte.getId(), e.getMessage());
        }
    }

    private Comparator<Reporte> construirComparador(String orden) {
        String valor = orden == null ? "recientes" : orden.toLowerCase();
        return switch (valor) {
            case "antiguos" -> Comparator.comparing((Reporte reporte) -> reporte.getFechaCreacion());
            case "prioridad" -> Comparator
                    .comparing((Reporte r) -> pesoGravedad(r.getGravedad()))
                    .reversed()
                    .thenComparing((Reporte reporte) -> reporte.getFechaCreacion());
            default -> Comparator.comparing((Reporte reporte) -> reporte.getFechaCreacion()).reversed();
        };
    }

    private int pesoGravedad(Gravedad gravedad) {
        if (gravedad == null) {
            return 0;
        }
        return switch (gravedad) {
            case BAJA -> 1;
            case MEDIA -> 2;
            case ALTA -> 3;
            case CRITICA -> 4;
        };
    }

    private long contarReportesActivosPrevios(String spotId, String resenaId) {
        if (resenaId != null && !resenaId.isBlank()) {
            return reporteRepository.countByResenaIdAndEstadoIn(resenaId, ESTADOS_ACTIVOS);
        }
        if (spotId != null && !spotId.isBlank()) {
            return reporteRepository.countBySpotIdAndEstadoIn(spotId, ESTADOS_ACTIVOS);
        }
        return 0;
    }

    // Priorización automática (Etapa 2, punto 6): la gravedad base depende de
    // la categoría, y sube automáticamente si el mismo objetivo (spot o
    // reseña) ya tiene reportes activos previos (reincidencia).
    private Gravedad calcularGravedad(CategoriaReporte categoria, long reportesActivosPrevios) {
        Gravedad base = switch (categoria) {
            case CONTENIDO_OFENSIVO, ERROR_TECNICO -> Gravedad.ALTA;
            case INFORMACION_INCORRECTA, PROBLEMA_SPOT -> Gravedad.MEDIA;
            case SPAM -> Gravedad.BAJA;
        };

        if (reportesActivosPrevios >= 3) {
            return Gravedad.CRITICA;
        }
        if (reportesActivosPrevios >= 1) {
            return subirUnNivel(base);
        }
        return base;
    }

    private Gravedad subirUnNivel(Gravedad gravedad) {
        return switch (gravedad) {
            case BAJA -> Gravedad.MEDIA;
            case MEDIA -> Gravedad.ALTA;
            case ALTA, CRITICA -> Gravedad.CRITICA;
        };
    }

    // Asignación automática (Etapa 1, punto 3):
    // - Error técnico -> ADMIN
    // - Contenido ofensivo, spam, información incorrecta y problema con spot -> MOD
    private Rol asignarResponsable(CategoriaReporte categoria) {
        return switch (categoria) {
            case ERROR_TECNICO -> Rol.ADMIN;
            case CONTENIDO_OFENSIVO, SPAM, INFORMACION_INCORRECTA, PROBLEMA_SPOT -> Rol.MOD;
        };
    }

    // Genera un ticket tipo "REP-482913" verificando unicidad contra la base de
    // datos
    private String generarNumeroTicketUnico() {
        String numeroTicket;
        do {
            String numeros = String.format("%06d", new Random().nextInt(999999));
            numeroTicket = "REP-" + numeros;
        } while (reporteRepository.existsByNumeroTicket(numeroTicket));
        return numeroTicket;
    }

    private ReporteResponseDTO mapearADTO(Reporte reporte) {
        List<ReporteResponseDTO.ObservacionDTO> bitacora = reporte.getBitacora() == null
                ? List.of()
                : reporte.getBitacora().stream()
                        .map(o -> ReporteResponseDTO.ObservacionDTO.builder()
                                .autor(o.getAutor())
                                .texto(o.getTexto())
                                .fecha(o.getFecha())
                                .build())
                        .toList();

        return ReporteResponseDTO.builder()
                .id(reporte.getId())
                .numeroTicket(reporte.getNumeroTicket())
                .categoria(reporte.getCategoria())
                .descripcion(reporte.getDescripcion())
                .evidencias(reporte.getEvidencias())
                .reportadoPor(reporte.getReportadoPor())
                .tipoObjetivo(reporte.getTipoObjetivo())
                .spotId(reporte.getSpotId())
                .nombreSpot(reporte.getNombreSpot())
                .esLocalDeSocio(reporte.getEsLocalDeSocio())
                .resenaId(reporte.getResenaId())
                .autorResenaReportada(reporte.getAutorResenaReportada())
                .asignadoA(reporte.getAsignadoA())
                .gravedad(reporte.getGravedad())
                .escalado(reporte.getEscalado())
                .fechaEscalado(reporte.getFechaEscalado())
                .escaladoPor(reporte.getEscaladoPor())
                .motivoEscalado(reporte.getMotivoEscalado())
                .actualizadoPor(reporte.getActualizadoPor())
                .resueltoPor(reporte.getResueltoPor())
                .estado(reporte.getEstado())
                .fechaCreacion(reporte.getFechaCreacion())
                .fechaActualizacion(reporte.getFechaActualizacion())
                .bitacora(bitacora)
                .build();
    }
}
