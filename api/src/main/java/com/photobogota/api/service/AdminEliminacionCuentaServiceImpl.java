package com.photobogota.api.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.photobogota.api.dto.DependenciasEliminacionDTO;
import com.photobogota.api.dto.MetricasEliminacionDTO;
import com.photobogota.api.dto.ProcesarEliminacionAdminDTO;
import com.photobogota.api.dto.SolicitudEliminacionAdminDTO;
import com.photobogota.api.exception.OperacionInvalidaException;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.model.EstadoReporte;
import com.photobogota.api.model.EstadoSolicitudEliminacion;
import com.photobogota.api.model.Notificacion;
import com.photobogota.api.model.NotificacionTipo;
import com.photobogota.api.model.Reporte;
import com.photobogota.api.model.SolicitudEliminacionCuenta;
import com.photobogota.api.model.Spot;
import com.photobogota.api.model.Usuario;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.CodigoEliminacionRepository;
import com.photobogota.api.repository.NotificacionRepository;
import com.photobogota.api.repository.RefreshTokenRepository;
import com.photobogota.api.repository.ReporteRepository;
import com.photobogota.api.repository.SesionRepository;
import com.photobogota.api.repository.SolicitudEliminacionRepository;
import com.photobogota.api.repository.SpotRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementación del panel de administración para procesar solicitudes de
 * eliminación de cuenta: verifica identidad automáticamente, resuelve
 * dependencias (spots y reportes pendientes), anonimiza los datos personales
 * de inmediato (sin esperar los 30 días), notifica a las partes afectadas y
 * expone métricas agregadas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEliminacionCuentaServiceImpl implements IAdminEliminacionCuentaService {

    private static final List<EstadoReporte> ESTADOS_REPORTE_PENDIENTES = List.of(
            EstadoReporte.NUEVO, EstadoReporte.EN_REVISION, EstadoReporte.PENDIENTE_VALIDACION);

    private final SolicitudEliminacionRepository solicitudEliminacionRepository;
    private final UsuarioAuthRepository usuarioAuthRepository;
    private final UsuarioRepository usuarioRepository;
    private final SpotRepository spotRepository;
    private final ReporteRepository reporteRepository;
    private final NotificacionRepository notificacionRepository;
    private final CodigoEliminacionRepository codigoEliminacionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SesionRepository sesionRepository;
    private final IEmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<SolicitudEliminacionAdminDTO> listarSolicitudes(EstadoSolicitudEliminacion estado,
            Pageable pageable) {
        Page<SolicitudEliminacionCuenta> pagina = estado != null
                ? solicitudEliminacionRepository.findByEstado(estado, pageable)
                : solicitudEliminacionRepository.findAll(pageable);
        return pagina.map(this::mapearADTO);
    }

    @Override
    public SolicitudEliminacionAdminDTO obtenerDetalle(String solicitudId) {
        return mapearADTO(buscarSolicitud(solicitudId));
    }

    @Override
    @Transactional
    public String procesarInmediatamente(String solicitudId, String adminUsername, ProcesarEliminacionAdminDTO dto) {
        SolicitudEliminacionCuenta solicitud = buscarSolicitud(solicitudId);

        if (solicitud.getEstado() == EstadoSolicitudEliminacion.COMPLETADA
                || solicitud.getEstado() == EstadoSolicitudEliminacion.CANCELADA) {
            throw new OperacionInvalidaException(
                    "Esta solicitud ya fue " + (solicitud.getEstado() == EstadoSolicitudEliminacion.COMPLETADA
                            ? "completada"
                            : "cancelada") + " previamente");
        }

        UsuarioAuth usuarioAuth = usuarioAuthRepository.findById(solicitud.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("El usuario asociado a esta solicitud ya no existe"));

        log.info("ADMIN {} procesando de forma inmediata la eliminación de la cuenta {}", adminUsername,
                solicitud.getNombreUsuario());

        // 1. Verificación automática de identidad: si los datos de la cuenta ya
        // no coinciden con los de la solicitud, se detiene el proceso para que
        // un humano lo revise en lugar de anonimizar a la cuenta equivocada.
        if (!verificarIdentidad(usuarioAuth, solicitud)) {
            throw new OperacionInvalidaException(
                    "No se pudo verificar automáticamente la identidad del solicitante: "
                            + "los datos de la cuenta no coinciden con los registrados en la solicitud");
        }

        // 2. Gestionar dependencias (spots, reportes pendientes) y notificar
        // a las partes afectadas.
        resolverDependencias(solicitud, adminUsername);

        // 3. Anonimizar los datos personales, conservando estadísticas generales.
        anonimizarCuenta(solicitud, usuarioAuth);

        // 4. Marcar la solicitud como completada.
        LocalDateTime ahora = LocalDateTime.now();
        solicitud.setEstado(EstadoSolicitudEliminacion.COMPLETADA);
        solicitud.setFechaCompletada(ahora);
        solicitud.setProcesadaManualmente(true);
        solicitud.setProcesadaPorAdmin(adminUsername);
        if (dto != null && dto.getObservacion() != null && !dto.getObservacion().isBlank()) {
            solicitud.setObservacionAdmin(dto.getObservacion());
        }
        solicitudEliminacionRepository.save(solicitud);

        log.info("Cuenta {} anonimizada manualmente por el administrador {}", solicitud.getNombreUsuario(),
                adminUsername);
        return "La cuenta fue anonimizada y la solicitud se marcó como completada";
    }

    @Override
    @Transactional
    public String rechazarSolicitud(String solicitudId, String adminUsername, ProcesarEliminacionAdminDTO dto) {
        SolicitudEliminacionCuenta solicitud = buscarSolicitud(solicitudId);

        if (solicitud.getEstado() != EstadoSolicitudEliminacion.PENDIENTE_VERIFICACION
                && solicitud.getEstado() != EstadoSolicitudEliminacion.PROGRAMADA) {
            throw new OperacionInvalidaException("Solo se pueden rechazar solicitudes activas");
        }

        solicitud.setEstado(EstadoSolicitudEliminacion.CANCELADA);
        solicitud.setFechaCancelacion(LocalDateTime.now());
        solicitud.setProcesadaPorAdmin(adminUsername);
        if (dto != null && dto.getObservacion() != null && !dto.getObservacion().isBlank()) {
            solicitud.setObservacionAdmin(dto.getObservacion());
        }
        solicitudEliminacionRepository.save(solicitud);

        usuarioRepository.findById(solicitud.getUsuarioId()).ifPresent(perfil -> {
            perfil.setEstadoCuenta(true);
            usuarioRepository.save(perfil);
        });
        codigoEliminacionRepository.deleteByEmail(solicitud.getEmail());

        String htmlContent = emailService.construirHtmlCancelacionEliminacion(solicitud.getNombreUsuario());
        emailService.enviarCorreoHtml(solicitud.getEmail(), "Tu cuenta fue reactivada - PhotoBogota", htmlContent);

        log.info("ADMIN {} rechazó la solicitud de eliminación de {}", adminUsername, solicitud.getNombreUsuario());
        return "La solicitud fue rechazada y la cuenta quedó activa nuevamente";
    }

    @Override
    public MetricasEliminacionDTO obtenerMetricas() {
        List<SolicitudEliminacionCuenta> todas = solicitudEliminacionRepository.findAll();

        Map<String, Long> porEstado = todas.stream()
                .collect(Collectors.groupingBy(s -> s.getEstado().name(), Collectors.counting()));

        Map<String, Long> porMotivo = todas.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getMotivo() != null ? s.getMotivo().name() : "SIN_ESPECIFICAR",
                        Collectors.counting()));

        Map<String, Long> porRol = todas.stream()
                .collect(Collectors.groupingBy(
                        s -> usuarioAuthRepository.findById(s.getUsuarioId())
                                .map(u -> u.getRol().name())
                                .orElse("DESCONOCIDO"),
                        Collectors.counting()));

        Double promedioDias = todas.stream()
                .filter(s -> s.getEstado() == EstadoSolicitudEliminacion.COMPLETADA
                        && s.getFechaConfirmacion() != null && s.getFechaCompletada() != null)
                .mapToLong(s -> Duration.between(s.getFechaConfirmacion(), s.getFechaCompletada()).toDays())
                .average()
                .stream().boxed().findFirst().orElse(null);

        LocalDateTime hace30Dias = LocalDateTime.now().minusDays(30);
        long completadasUltimos30Dias = todas.stream()
                .filter(s -> s.getEstado() == EstadoSolicitudEliminacion.COMPLETADA
                        && s.getFechaCompletada() != null && s.getFechaCompletada().isAfter(hace30Dias))
                .count();

        long procesadasManualmente = todas.stream()
                .filter(s -> Boolean.TRUE.equals(s.getProcesadaManualmente()))
                .count();

        return MetricasEliminacionDTO.builder()
                .totalSolicitudes(todas.size())
                .porEstado(porEstado)
                .porMotivo(porMotivo)
                .porRol(porRol)
                .promedioDiasHastaCompletada(promedioDias)
                .completadasUltimos30Dias(completadasUltimos30Dias)
                .procesadasManualmentePorAdmin(procesadasManualmente)
                .build();
    }

    // ─────────────────────────── Helpers privados ───────────────────────────

    private SolicitudEliminacionCuenta buscarSolicitud(String solicitudId) {
        ObjectId id;
        try {
            id = new ObjectId(solicitudId);
        } catch (IllegalArgumentException e) {
            throw new OperacionInvalidaException("El identificador de la solicitud no es válido");
        }
        return solicitudEliminacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la solicitud de eliminación"));
    }

    /**
     * Verificación automática de identidad: confirma que la cuenta que
     * originó la solicitud sigue siendo la misma (mismo email y nombre de
     * usuario), evitando procesar por error una cuenta distinta.
     */
    private boolean verificarIdentidad(UsuarioAuth usuarioAuth, SolicitudEliminacionCuenta solicitud) {
        return usuarioAuth.getEmail() != null
                && usuarioAuth.getEmail().equalsIgnoreCase(solicitud.getEmail())
                && usuarioAuth.getNombreUsuario() != null
                && usuarioAuth.getNombreUsuario().equals(solicitud.getNombreUsuario());
    }

    private DependenciasEliminacionDTO calcularDependencias(ObjectId usuarioId, String nombreUsuario) {
        List<Spot> spots = spotRepository.findByCreadorUsername(nombreUsuario);
        List<String> spotIds = spots.stream().map(spot -> spot.getId()).toList();

        int reportesComoAutor = reporteRepository
                .findByReportadoPorAndEstadoIn(nombreUsuario, ESTADOS_REPORTE_PENDIENTES).size();

        int reportesSobreContenido = 0;
        if (!spotIds.isEmpty()) {
            reportesSobreContenido += reporteRepository
                    .findBySpotIdInAndEstadoIn(spotIds, ESTADOS_REPORTE_PENDIENTES).size();
        }
        reportesSobreContenido += reporteRepository
                .findByAutorResenaReportadaAndEstadoIn(nombreUsuario, ESTADOS_REPORTE_PENDIENTES).size();

        boolean tienePendientes = !spots.isEmpty() || reportesComoAutor > 0 || reportesSobreContenido > 0;

        return DependenciasEliminacionDTO.builder()
                .spotsCreados(spots.size())
                .reportesPendientesComoAutor(reportesComoAutor)
                .reportesPendientesSobreSuContenido(reportesSobreContenido)
                .tieneDependenciasPendientes(tienePendientes)
                .build();
    }

    /**
     * Cierra automáticamente los reportes pendientes relacionados con el
     * usuario (los que él presentó y los que existen sobre su contenido),
     * notifica a quienes los presentaron, y anonimiza al creador de sus
     * spots sin borrarlos (para conservar rating y reseñas como estadística
     * general).
     */
    private void resolverDependencias(SolicitudEliminacionCuenta solicitud, String adminUsername) {
        String nombreUsuario = solicitud.getNombreUsuario();
        String sufijo = solicitud.getUsuarioId().toHexString();

        List<Spot> spots = spotRepository.findByCreadorUsername(nombreUsuario);
        List<String> spotIds = spots.stream().map(spot -> spot.getId()).toList();

        Set<Reporte> reportesPendientes = new LinkedHashSet<>();
        reportesPendientes.addAll(
                reporteRepository.findByReportadoPorAndEstadoIn(nombreUsuario, ESTADOS_REPORTE_PENDIENTES));
        if (!spotIds.isEmpty()) {
            reportesPendientes.addAll(
                    reporteRepository.findBySpotIdInAndEstadoIn(spotIds, ESTADOS_REPORTE_PENDIENTES));
        }
        reportesPendientes.addAll(
                reporteRepository.findByAutorResenaReportadaAndEstadoIn(nombreUsuario, ESTADOS_REPORTE_PENDIENTES));

        LocalDateTime ahora = LocalDateTime.now();
        for (Reporte reporte : reportesPendientes) {
            reporte.setEstado(EstadoReporte.RESUELTO);
            reporte.setActualizadoPor(adminUsername);
            reporte.setFechaActualizacion(ahora);
            if (reporte.getBitacora() == null) {
                reporte.setBitacora(new ArrayList<>());
            }
            reporte.getBitacora().add(Reporte.Observacion.builder()
                    .autor(adminUsername)
                    .texto("Cerrado automáticamente: la cuenta asociada a este reporte fue eliminada.")
                    .fecha(ahora)
                    .build());
            reporteRepository.save(reporte);

            // Notificar a la parte afectada (quien presentó el reporte), salvo
            // que sea el propio usuario que se está eliminando.
            if (reporte.getReportadoPor() != null && !reporte.getReportadoPor().equals(nombreUsuario)) {
                notificacionRepository.save(Notificacion.builder()
                        .destinatarioUsername(reporte.getReportadoPor())
                        .tipo(NotificacionTipo.SISTEMA)
                        .titulo("Tu reporte fue cerrado")
                        .mensaje("Tu reporte " + reporte.getNumeroTicket()
                                + " se cerró automáticamente porque la cuenta involucrada fue eliminada.")
                        .emisorUsername("sistema")
                        .build());
            }
        }

        // Anonimizar al creador de los spots sin borrarlos: se conserva el
        // rating, las reseñas y el spot en sí como estadística general.
        if (!spots.isEmpty()) {
            String creadorAnonimo = "usuario_eliminado_" + sufijo;
            for (Spot spot : spots) {
                spot.setCreadorUsername(creadorAnonimo);
                spotRepository.save(spot);
            }
            log.info("{} spot(s) del usuario {} reasignados al creador anónimo {}", spots.size(), nombreUsuario,
                    creadorAnonimo);
        }
    }

    /**
     * Anonimiza los datos personales identificables del usuario (mismo
     * criterio que el job automático de la Etapa 1), sin tocar puntos,
     * nivel, calificaciones ni el contenido ya creado.
     */
    private void anonimizarCuenta(SolicitudEliminacionCuenta solicitud, UsuarioAuth usuarioAuth) {
        Usuario perfil = usuarioRepository.findById(solicitud.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el perfil del usuario a anonimizar"));

        String emailOriginal = usuarioAuth.getEmail();
        String sufijo = solicitud.getUsuarioId().toHexString();

        perfil.setNombresCompletos("Usuario eliminado");
        perfil.setTelefono(null);
        perfil.setFotoPerfil(null);
        perfil.setBiografia(null);
        perfil.setEstadoCuenta(false);
        usuarioRepository.save(perfil);

        usuarioAuth.setEmail("eliminado_" + sufijo + "@anonimizado.photobogota.com");
        usuarioAuth.setNombreUsuario("usuario_eliminado_" + sufijo);
        usuarioAuth.setContrasena(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuarioAuthRepository.save(usuarioAuth);

        refreshTokenRepository.deleteByEmail(emailOriginal);
        sesionRepository.deleteByUsuarioId(solicitud.getUsuarioId().toString());

        String htmlContent = emailService.construirHtmlEliminacionCompletada(solicitud.getNombreUsuario());
        emailService.enviarCorreoHtml(emailOriginal, "Tu cuenta fue eliminada - PhotoBogota", htmlContent);
    }

    private SolicitudEliminacionAdminDTO mapearADTO(SolicitudEliminacionCuenta solicitud) {
        UsuarioAuth usuarioAuth = usuarioAuthRepository.findById(solicitud.getUsuarioId()).orElse(null);

        Long diasRestantes = null;
        if (solicitud.getEstado() == EstadoSolicitudEliminacion.PROGRAMADA
                && solicitud.getFechaProgramadaEliminacion() != null) {
            diasRestantes = Duration.between(LocalDateTime.now(), solicitud.getFechaProgramadaEliminacion()).toDays();
            if (diasRestantes < 0) {
                diasRestantes = 0L;
            }
        }

        return SolicitudEliminacionAdminDTO.builder()
                .id(solicitud.getId().toHexString())
                .usuarioId(solicitud.getUsuarioId().toHexString())
                .nombreUsuario(solicitud.getNombreUsuario())
                .email(solicitud.getEmail())
                .rol(usuarioAuth != null ? usuarioAuth.getRol().name() : "DESCONOCIDO")
                .motivo(solicitud.getMotivo())
                .comentario(solicitud.getComentario())
                .estado(solicitud.getEstado().name())
                .fechaSolicitud(solicitud.getFechaSolicitud())
                .fechaConfirmacion(solicitud.getFechaConfirmacion())
                .fechaProgramadaEliminacion(solicitud.getFechaProgramadaEliminacion())
                .fechaCancelacion(solicitud.getFechaCancelacion())
                .fechaCompletada(solicitud.getFechaCompletada())
                .diasRestantes(diasRestantes)
                .identidadVerificada(usuarioAuth != null && verificarIdentidad(usuarioAuth, solicitud))
                .dependencias(calcularDependencias(solicitud.getUsuarioId(), solicitud.getNombreUsuario()))
                .procesadaManualmente(Boolean.TRUE.equals(solicitud.getProcesadaManualmente()))
                .procesadaPorAdmin(solicitud.getProcesadaPorAdmin())
                .observacionAdmin(solicitud.getObservacionAdmin())
                .build();
    }
}
