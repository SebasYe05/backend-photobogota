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

@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements IReporteService {

    // Reportes en estos estados cuentan como "activos" para calcular reincidencia
    private static final List<EstadoReporte> ESTADOS_ACTIVOS = List.of(
            EstadoReporte.NUEVO, EstadoReporte.EN_REVISION, EstadoReporte.PENDIENTE_VALIDACION);

    private final ReporteRepository reporteRepository;
    private final SpotRepository spotRepository;
    private final CalificacionRepository calificacionRepository;
    private final MongoTemplate mongoTemplate;
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

        boolean esLocalDeSocio = spot != null && "SOCIO".equals(spot.getCreadorRol());

        long reportesActivosPrevios = contarReportesActivosPrevios(request.getSpotId(), request.getResenaId());
        Gravedad gravedad = calcularGravedad(request.getCategoria(), reportesActivosPrevios);

        Rol asignadoA = asignarResponsable(request.getCategoria(), tipoObjetivo, esLocalDeSocio);

        LocalDateTime ahora = LocalDateTime.now();

        Reporte reporte = Reporte.builder()
                .numeroTicket(generarNumeroTicketUnico())
                .categoria(request.getCategoria())
                .descripcion(request.getDescripcion())
                .evidencias(request.getEvidencias() != null ? request.getEvidencias() : List.of())
                .reportadoPor(usuario)
                .tipoObjetivo(tipoObjetivo)
                .spotId(request.getSpotId())
                .nombreSpot(spot != null ? spot.getNombre() : null)
                .esLocalDeSocio(spot != null ? esLocalDeSocio : null)
                .propietarioSocio(esLocalDeSocio ? spot.getCreadorUsername() : null)
                .resenaId(calificacionReportada != null ? calificacionReportada.getId() : null)
                .autorResenaReportada(calificacionReportada != null ? calificacionReportada.getUsuario() : null)
                .asignadoA(asignadoA)
                .gravedad(gravedad)
                .estado(EstadoReporte.NUEVO)
                .fechaCreacion(ahora)
                // Plazos de atención del socio (HU 15 pt 6): responder en 24h,
                // resolver en 5 días. Solo aplica cuando queda en su cola.
                .fechaLimiteRespuesta(asignadoA == Rol.SOCIO ? ahora.plusHours(24) : null)
                .fechaLimiteResolucion(asignadoA == Rol.SOCIO ? ahora.plusDays(5) : null)
                .build();

        Reporte guardado = reporteRepository.save(reporte);

        // HU 6 pt 4: confirmación con número de ticket al reportante.
        notificacionService.notificarSistema(usuario, "Reporte recibido: " + guardado.getNumeroTicket(),
                "Registramos tu reporte con el ticket " + guardado.getNumeroTicket()
                        + ". Te avisaremos cuando se resuelva.");

        // HU 6 pt 3 / HU 15 pt 1 / HU 16 pt 7: avisar de inmediato a quien
        // le corresponde atenderlo.
        notificarNuevaAsignacion(guardado);

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
    public List<ReporteResponseDTO> listarPorRolAsignado(Rol rol, String username) {
        List<Reporte> reportes = rol == Rol.SOCIO
                ? reporteRepository.findByAsignadoAAndPropietarioSocio(Rol.SOCIO, username)
                : reporteRepository.findByAsignadoA(rol);
        return reportes.stream()
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

        // ADMIN solo ve lo que le fue asignado a ADMIN: los reportes de
        // categoría ERROR_TECNICO (asignación automática) y los que un
        // moderador escaló (escalar() cambia asignadoA a ADMIN). NO ve
        // la cola de MOD que todavía no fue escalada.
        // MOD solo ve su propia cola (lo que sigue asignado a MOD).
        // SOCIO solo ve su propia cola Y solo de sus propios locales (no
        // los de otros socios).
        query.addCriteria(Criteria.where(Reporte.Fields.asignadoA).is(rolUsuario));

        if (rolUsuario == Rol.SOCIO) {
            query.addCriteria(Criteria.where(Reporte.Fields.propietarioSocio).is(username));
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

        if (reporte.getEstado() == EstadoReporte.PENDIENTE_VALIDACION) {
            throw new OperacionInvalidaException(
                    "Este reporte está pendiente de validación de un moderador y no se puede modificar");
        }

        EstadoReporte nuevoEstado = request.getEstado();

        // HU 15 pt 4-5 / HU 16 pt 4-5: cuando un SOCIO o un ADMIN marcan un
        // reporte como Solucionado, no queda resuelto de inmediato: pasa a
        // validación de un MOD, que es quien recién notifica al miembro.
        // Un MOD que resuelve su propia cola no necesita que nadie más lo
        // valide, así que ahí sí queda RESUELTO directo.
        boolean requiereValidacion = nuevoEstado == EstadoReporte.RESUELTO
                && (rolUsuario == Rol.SOCIO || rolUsuario == Rol.ADMIN);

        if (requiereValidacion) {
            reporte.setEstado(EstadoReporte.PENDIENTE_VALIDACION);
            reporte.setResueltoPor(usuario);
        } else {
            reporte.setEstado(nuevoEstado);
        }

        reporte.setActualizadoPor(usuario);
        reporte.setFechaActualizacion(LocalDateTime.now());

        if (request.getObservacion() != null && !request.getObservacion().isBlank()) {
            agregarObservacion(reporte, usuario, request.getObservacion());
        }

        Reporte actualizado = reporteRepository.save(reporte);

        if (requiereValidacion) {
            notificacionService.notificarPorRol(Rol.MOD,
                    "Reporte pendiente de validar: " + actualizado.getNumeroTicket(),
                    usuario + " marcó el reporte " + actualizado.getNumeroTicket()
                            + " como solucionado. Valídalo para notificar al miembro afectado.",
                    usuario);
        } else if (nuevoEstado == EstadoReporte.RESUELTO || nuevoEstado == EstadoReporte.RECHAZADO) {
            // HU 6 pt 5: notificar la resolución al usuario que reportó.
            notificarResolucionAlReportante(actualizado);
        }

        return mapearADTO(actualizado);
    }

    @Override
    public List<ReporteResponseDTO> listarPendientesValidacion() {
        return reporteRepository.findByEstado(EstadoReporte.PENDIENTE_VALIDACION).stream()
                .map(this::mapearADTO)
                .toList();
    }

    @Override
    public ReporteResponseDTO validarReporte(String id, ValidarReporteRequestDTO request, String usuarioMod) {
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado con id: " + id));

        if (reporte.getEstado() != EstadoReporte.PENDIENTE_VALIDACION) {
            throw new OperacionInvalidaException("Este reporte no está pendiente de validación");
        }

        reporte.setValidadoPor(usuarioMod);
        reporte.setFechaValidacion(LocalDateTime.now());
        reporte.setActualizadoPor(usuarioMod);
        reporte.setFechaActualizacion(LocalDateTime.now());

        if (request.getObservacion() != null && !request.getObservacion().isBlank()) {
            agregarObservacion(reporte, usuarioMod, request.getObservacion());
        }

        if (Boolean.TRUE.equals(request.getAprobado())) {
            reporte.setEstado(EstadoReporte.RESUELTO);
            Reporte actualizado = reporteRepository.save(reporte);

            // HU 15 pt 5 / HU 16 pt 5: al aprobar el moderador, se notifica
            // automáticamente al miembro afectado.
            notificarResolucionAlReportante(actualizado);
            return mapearADTO(actualizado);
        }

        // Rechazada: vuelve a la cola de quien la propuso para que la revise.
        reporte.setEstado(EstadoReporte.EN_REVISION);
        Reporte actualizado = reporteRepository.save(reporte);

        if (actualizado.getResueltoPor() != null) {
            notificacionService.notificarSistema(actualizado.getResueltoPor(),
                    "Solución rechazada: " + actualizado.getNumeroTicket(),
                    "Un moderador no aprobó la solución del reporte " + actualizado.getNumeroTicket()
                            + (request.getObservacion() != null && !request.getObservacion().isBlank()
                                    ? ". Motivo: " + request.getObservacion()
                                    : ". Revisalo de nuevo."));
        }

        return mapearADTO(actualizado);
    }

    @Override
    public ReporteResponseDTO escalarReporte(String id, EscalarReporteRequestDTO request, String usuario,
            Rol rolUsuario) {
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado con id: " + id));

        Rol siguienteNivel = determinarSiguienteNivelEscalamiento(reporte, rolUsuario);

        validarPropiedad(reporte, rolUsuario, usuario);

        registrarEscalamiento(reporte, rolUsuario, siguienteNivel, usuario, request.getMotivo(), false);

        Reporte actualizado = reporteRepository.save(reporte);

        notificacionService.notificarPorRol(siguienteNivel,
                "Reporte escalado: " + actualizado.getNumeroTicket(),
                usuario + " te escaló el reporte " + actualizado.getNumeroTicket()
                        + (request.getMotivo() != null && !request.getMotivo().isBlank()
                                ? ". Motivo: " + request.getMotivo()
                                : "."),
                usuario);

        return mapearADTO(actualizado);
    }

    // Un SOCIO solo puede escalar lo suyo (a MOD). Un MOD solo lo suyo (a
    // ADMIN). ADMIN es el techo de la cadena: no hay a quién escalarle.
    private Rol determinarSiguienteNivelEscalamiento(Reporte reporte, Rol rolUsuario) {
        if (rolUsuario == Rol.SOCIO) {
            if (reporte.getAsignadoA() != Rol.SOCIO) {
                throw new AccessForbiddenException("Este reporte no está asignado a tu local");
            }
            return Rol.MOD;
        }
        if (rolUsuario == Rol.MOD) {
            if (reporte.getAsignadoA() != Rol.MOD) {
                throw new AccessForbiddenException("Este reporte no está asignado a moderación");
            }
            return Rol.ADMIN;
        }
        throw new AccessForbiddenException("Tu rol no puede escalar reportes");
    }

    private void registrarEscalamiento(Reporte reporte, Rol de, Rol a, String por, String motivo,
            boolean automatico) {
        LocalDateTime ahora = LocalDateTime.now();

        if (reporte.getHistorialEscalamiento() == null) {
            reporte.setHistorialEscalamiento(new ArrayList<>());
        }
        reporte.getHistorialEscalamiento().add(Reporte.Escalamiento.builder()
                .de(de)
                .a(a)
                .por(por)
                .motivo(motivo)
                .automatico(automatico)
                .fecha(ahora)
                .build());

        reporte.setEscalado(true);
        reporte.setFechaEscalado(ahora);
        reporte.setEscaladoPor(por);
        reporte.setMotivoEscalado(motivo);
        reporte.setEscaladoAutomaticamente(automatico);
        reporte.setAsignadoA(a);
        // Un reporte escalado pasa a ser prioritario en el dashboard del
        // siguiente nivel.
        reporte.setGravedad(Gravedad.CRITICA);
        reporte.setFechaActualizacion(ahora);
    }

    // Escalamiento automático (HU 15 pt 7): sube a MOD los reportes de un
    // SOCIO que llevan más de 24h sin respuesta. Lo llama el scheduler.
    public void escalarVencidosAutomaticamente() {
        List<Reporte> vencidos = reporteRepository.findByAsignadoAAndEstadoAndFechaLimiteRespuestaBefore(
                Rol.SOCIO, EstadoReporte.NUEVO, LocalDateTime.now());

        for (Reporte reporte : vencidos) {
            registrarEscalamiento(reporte, Rol.SOCIO, Rol.MOD, "sistema",
                    "Escalamiento automático: el socio no respondió dentro del plazo de 24 horas", true);
            Reporte actualizado = reporteRepository.save(reporte);

            notificacionService.notificarPorRol(Rol.MOD,
                    "Reporte escalado automáticamente: " + actualizado.getNumeroTicket(),
                    "El socio no respondió a tiempo el reporte " + actualizado.getNumeroTicket()
                            + " y se escaló automáticamente.",
                    "sistema");

            if (actualizado.getPropietarioSocio() != null) {
                notificacionService.notificarSistema(actualizado.getPropietarioSocio(),
                        "Reporte escalado por falta de respuesta: " + actualizado.getNumeroTicket(),
                        "No respondiste a tiempo el reporte " + actualizado.getNumeroTicket()
                                + " (máximo 24h) y se escaló a moderación.");
            }
        }
    }

    // Verifica que quien actúa sobre un reporte tenga permiso: un SOCIO solo
    // sobre reportes de su propia cola Y de sus propios locales, un MOD solo
    // sobre los que le pertenecen a su cola. ADMIN tiene permiso sobre
    // cualquier reporte (oversight).
    private void validarPropiedad(Reporte reporte, Rol rolUsuario, String usuario) {
        if (rolUsuario == Rol.MOD && reporte.getAsignadoA() != Rol.MOD) {
            throw new AccessForbiddenException("Este reporte no está asignado a moderación");
        }
        if (rolUsuario == Rol.SOCIO) {
            if (reporte.getAsignadoA() != Rol.SOCIO || !usuario.equals(reporte.getPropietarioSocio())) {
                throw new AccessForbiddenException("Este reporte no está asignado a tu local");
            }
        }
    }

    private void agregarObservacion(Reporte reporte, String autor, String texto) {
        if (reporte.getBitacora() == null) {
            reporte.setBitacora(new ArrayList<>());
        }
        reporte.getBitacora().add(Reporte.Observacion.builder()
                .autor(autor)
                .texto(texto)
                .fecha(LocalDateTime.now())
                .build());
    }

    private void notificarResolucionAlReportante(Reporte reporte) {
        if (reporte.getReportadoPor() == null) {
            return;
        }
        boolean resuelto = reporte.getEstado() == EstadoReporte.RESUELTO;
        String titulo = (resuelto ? "Reporte resuelto: " : "Reporte rechazado: ") + reporte.getNumeroTicket();
        String mensaje = resuelto
                ? "Tu reporte " + reporte.getNumeroTicket() + " sobre \""
                        + (reporte.getNombreSpot() != null ? reporte.getNombreSpot() : "el contenido reportado")
                        + "\" fue resuelto."
                : "Tu reporte " + reporte.getNumeroTicket() + " fue revisado y no se encontraron irregularidades.";
        notificacionService.notificarSistema(reporte.getReportadoPor(), titulo, mensaje);
    }

    private void notificarNuevaAsignacion(Reporte reporte) {
        String titulo = "Nuevo reporte asignado: " + reporte.getNumeroTicket();
        String mensaje = "Se te asignó el reporte " + reporte.getNumeroTicket() + " ("
                + reporte.getCategoria() + ")"
                + (reporte.getNombreSpot() != null ? " sobre \"" + reporte.getNombreSpot() + "\"" : "") + ".";

        if (reporte.getAsignadoA() == Rol.SOCIO) {
            if (reporte.getPropietarioSocio() != null) {
                notificacionService.notificarSistema(reporte.getPropietarioSocio(), titulo,
                        mensaje + " Tienes 24h para responder y 5 días para resolverlo.");
            }
            return;
        }

        notificacionService.notificarPorRol(reporte.getAsignadoA(), titulo, mensaje, "sistema");
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

    // Asignación automática (HU 6 pt 3, corregida para HU 15/HU 24):
    // - Local de un SOCIO (esLocalDeSocio=true) -> SOCIO, sin importar la
    //   categoría: el dueño del local es quien primero debe atenderlo.
    // - Cualquier otro reporte sobre un spot (tipoObjetivo=SPOT, no es local
    //   de socio) -> MOD, sin importar la categoría: TODO reporte de un spot
    //   le debe llegar al moderador.
    // - Reseñas y reportes sin spot asociado (ej: error técnico general de
    //   la app) -> se asignan por categoría: error técnico a ADMIN, el
    //   resto a MOD.
    private Rol asignarResponsable(CategoriaReporte categoria, TipoObjetivoReporte tipoObjetivo,
            boolean esLocalDeSocio) {
        if (esLocalDeSocio) {
            return Rol.SOCIO;
        }
        if (tipoObjetivo == TipoObjetivoReporte.SPOT) {
            return Rol.MOD;
        }
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

        List<ReporteResponseDTO.EscalamientoDTO> historial = reporte.getHistorialEscalamiento() == null
                ? List.of()
                : reporte.getHistorialEscalamiento().stream()
                        .map(e -> ReporteResponseDTO.EscalamientoDTO.builder()
                                .de(e.getDe())
                                .a(e.getA())
                                .por(e.getPor())
                                .motivo(e.getMotivo())
                                .automatico(e.getAutomatico())
                                .fecha(e.getFecha())
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
                .propietarioSocio(reporte.getPropietarioSocio())
                .resenaId(reporte.getResenaId())
                .autorResenaReportada(reporte.getAutorResenaReportada())
                .asignadoA(reporte.getAsignadoA())
                .gravedad(reporte.getGravedad())
                .escalado(reporte.getEscalado())
                .fechaEscalado(reporte.getFechaEscalado())
                .escaladoPor(reporte.getEscaladoPor())
                .motivoEscalado(reporte.getMotivoEscalado())
                .escaladoAutomaticamente(reporte.getEscaladoAutomaticamente())
                .historialEscalamiento(historial)
                .resueltoPor(reporte.getResueltoPor())
                .validadoPor(reporte.getValidadoPor())
                .fechaValidacion(reporte.getFechaValidacion())
                .fechaLimiteRespuesta(reporte.getFechaLimiteRespuesta())
                .fechaLimiteResolucion(reporte.getFechaLimiteResolucion())
                .actualizadoPor(reporte.getActualizadoPor())
                .estado(reporte.getEstado())
                .fechaCreacion(reporte.getFechaCreacion())
                .fechaActualizacion(reporte.getFechaActualizacion())
                .bitacora(bitacora)
                .build();
    }
}
