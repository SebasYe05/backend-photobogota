package com.photobogota.api.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.photobogota.api.dto.ConfirmarEliminacionDTO;
import com.photobogota.api.dto.EstadoEliminacionDTO;
import com.photobogota.api.dto.SolicitarEliminacionDTO;
import com.photobogota.api.exception.AccessForbiddenException;
import com.photobogota.api.exception.InvalidCredentialsException;
import com.photobogota.api.exception.OperacionInvalidaException;
import com.photobogota.api.model.CodigoEliminacionCuenta;
import com.photobogota.api.model.EstadoSolicitudEliminacion;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.SolicitudEliminacionCuenta;
import com.photobogota.api.model.Usuario;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.CodigoEliminacionRepository;
import com.photobogota.api.repository.RefreshTokenRepository;
import com.photobogota.api.repository.SesionRepository;
import com.photobogota.api.repository.SolicitudEliminacionRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementación del servicio de autoeliminación de cuenta para MIEMBROS.
 * Sigue el mismo patrón que la recuperación de contraseña: código numérico
 * de 6 dígitos guardado en Mongo y enviado por correo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EliminacionCuentaServiceImpl implements IEliminacionCuentaService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter
            .ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-ES"));

    private static final List<EstadoSolicitudEliminacion> ESTADOS_ACTIVOS = List.of(
            EstadoSolicitudEliminacion.PENDIENTE_VERIFICACION,
            EstadoSolicitudEliminacion.PROGRAMADA);

    private final UsuarioAuthRepository usuarioAuthRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudEliminacionRepository solicitudEliminacionRepository;
    private final CodigoEliminacionRepository codigoEliminacionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SesionRepository sesionRepository;
    private final IEmailService emailService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Genera un código de verificación y lo envía por correo junto con las
     * consecuencias de eliminar la cuenta. Solo disponible para MIEMBRO.
     */
    @Override
    @Transactional
    public String solicitarEliminacion(String nombreUsuario, SolicitarEliminacionDTO dto) {
        log.info("Solicitud de eliminación de cuenta iniciada por: {}", nombreUsuario);

        UsuarioAuth usuarioAuth = usuarioAuthRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado"));

        if (usuarioAuth.getRol() != Rol.MIEMBRO) {
            throw new AccessForbiddenException(
                    "La autoeliminación de cuenta solo está disponible para usuarios con rol MIEMBRO");
        }

        // Evitar solicitudes duplicadas si ya hay una en curso
        solicitudEliminacionRepository.findByUsuarioIdAndEstadoIn(usuarioAuth.getId(), ESTADOS_ACTIVOS)
                .ifPresent(solicitud -> {
                    throw new OperacionInvalidaException(
                            "Ya tienes una solicitud de eliminación de cuenta en curso");
                });

        String email = usuarioAuth.getEmail();

        // Limpiar códigos anteriores del mismo email
        codigoEliminacionRepository.deleteByEmail(email);

        // Generar código numérico de 6 dígitos
        String codigo = String.format("%06d", new java.util.Random().nextInt(1000000));

        LocalDateTime ahora = LocalDateTime.now();
        CodigoEliminacionCuenta codigoEliminacion = CodigoEliminacionCuenta.builder()
                .email(email)
                .codigo(codigo)
                .fechaCreacion(ahora)
                .fechaExpiracion(ahora.plusMinutes(15))
                .usado(false)
                .build();
        codigoEliminacionRepository.save(codigoEliminacion);

        // Registrar (o reemplazar) la solicitud pendiente de verificación
        SolicitudEliminacionCuenta solicitud = SolicitudEliminacionCuenta.builder()
                .usuarioId(usuarioAuth.getId())
                .email(email)
                .nombreUsuario(usuarioAuth.getNombreUsuario())
                .motivo(dto.getMotivo())
                .comentario(dto.getComentario())
                .estado(EstadoSolicitudEliminacion.PENDIENTE_VERIFICACION)
                .fechaSolicitud(ahora)
                .build();
        solicitudEliminacionRepository.save(solicitud);

        String htmlContent = emailService.construirHtmlSolicitudEliminacion(nombreUsuario, codigo);
        emailService.enviarCorreoHtml(email, "Eliminar cuenta - PhotoBogota", htmlContent);

        log.info("Código de eliminación de cuenta enviado exitosamente a: {}", email);
        return "Te enviamos un código de verificación a tu correo electrónico para confirmar la eliminación de tu cuenta";
    }

    /**
     * Valida el código de verificación, desactiva la cuenta y programa su
     * anonimización definitiva dentro de 30 días.
     */
    @Override
    @Transactional
    public String confirmarEliminacion(String nombreUsuario, ConfirmarEliminacionDTO dto) {
        log.info("Confirmando eliminación de cuenta para: {}", nombreUsuario);

        UsuarioAuth usuarioAuth = usuarioAuthRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado"));

        String email = usuarioAuth.getEmail();

        CodigoEliminacionCuenta codigo = codigoEliminacionRepository
                .findByEmailAndCodigo(email, dto.getCodigo())
                .orElseThrow(() -> new InvalidCredentialsException("Código de verificación inválido"));

        if (codigo.isUsado()) {
            throw new InvalidCredentialsException("El código de verificación ya ha sido utilizado");
        }

        if (codigo.estaExpirado()) {
            throw new InvalidCredentialsException(
                    "El código de verificación ha expirado. Por favor, solicita uno nuevo");
        }

        SolicitudEliminacionCuenta solicitud = solicitudEliminacionRepository
                .findByUsuarioIdAndEstadoIn(usuarioAuth.getId(),
                        List.of(EstadoSolicitudEliminacion.PENDIENTE_VERIFICACION))
                .orElseThrow(() -> new OperacionInvalidaException(
                        "No tienes ninguna solicitud de eliminación pendiente de confirmación"));

        codigo.setUsado(true);
        codigoEliminacionRepository.save(codigo);

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fechaProgramada = ahora.plusDays(30);

        solicitud.setEstado(EstadoSolicitudEliminacion.PROGRAMADA);
        solicitud.setFechaConfirmacion(ahora);
        solicitud.setFechaProgramadaEliminacion(fechaProgramada);
        solicitudEliminacionRepository.save(solicitud);

        // Desactivar la cuenta de inmediato y cerrar todas sus sesiones activas
        Usuario perfil = usuarioRepository.findById(usuarioAuth.getId())
                .orElseThrow(() -> new InvalidCredentialsException("Perfil de usuario no encontrado"));
        perfil.setEstadoCuenta(false);
        usuarioRepository.save(perfil);

        refreshTokenRepository.deleteByEmail(email);
        sesionRepository.deleteByUsuarioId(usuarioAuth.getId().toString());

        String fechaFormateada = fechaProgramada.format(FORMATO_FECHA);
        String htmlContent = emailService.construirHtmlConfirmacionEliminacion(nombreUsuario, fechaFormateada);
        emailService.enviarCorreoHtml(email, "Eliminación de cuenta confirmada - PhotoBogota", htmlContent);

        log.info("Eliminación de cuenta programada para {} el {}", nombreUsuario, fechaProgramada);
        return "Tu cuenta fue desactivada. Tus datos se eliminarán definitivamente el " + fechaFormateada
                + " a menos que inicies sesión antes de esa fecha para recuperarla";
    }

    /**
     * Cancela una solicitud de eliminación dentro del período de 30 días y
     * reactiva la cuenta.
     */
    @Override
    @Transactional
    public String cancelarEliminacion(String nombreUsuario) {
        log.info("Cancelando eliminación de cuenta para: {}", nombreUsuario);

        UsuarioAuth usuarioAuth = usuarioAuthRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado"));

        SolicitudEliminacionCuenta solicitud = solicitudEliminacionRepository
                .findByUsuarioIdAndEstadoIn(usuarioAuth.getId(), ESTADOS_ACTIVOS)
                .orElseThrow(() -> new OperacionInvalidaException(
                        "No tienes ninguna solicitud de eliminación activa para cancelar"));

        if (solicitud.getEstado() == EstadoSolicitudEliminacion.PROGRAMADA
                && solicitud.getFechaProgramadaEliminacion() != null
                && LocalDateTime.now().isAfter(solicitud.getFechaProgramadaEliminacion())) {
            throw new OperacionInvalidaException(
                    "El plazo de 30 días para recuperar tu cuenta ya venció");
        }

        solicitud.setEstado(EstadoSolicitudEliminacion.CANCELADA);
        solicitud.setFechaCancelacion(LocalDateTime.now());
        solicitudEliminacionRepository.save(solicitud);

        Usuario perfil = usuarioRepository.findById(usuarioAuth.getId())
                .orElseThrow(() -> new InvalidCredentialsException("Perfil de usuario no encontrado"));
        perfil.setEstadoCuenta(true);
        usuarioRepository.save(perfil);

        codigoEliminacionRepository.deleteByEmail(usuarioAuth.getEmail());

        String htmlContent = emailService.construirHtmlCancelacionEliminacion(nombreUsuario);
        emailService.enviarCorreoHtml(usuarioAuth.getEmail(), "Tu cuenta fue reactivada - PhotoBogota", htmlContent);

        log.info("Eliminación de cuenta cancelada para: {}", nombreUsuario);
        return "Tu solicitud de eliminación fue cancelada y tu cuenta está activa nuevamente";
    }

    /**
     * Devuelve el estado actual de la solicitud de eliminación del usuario,
     * si tiene alguna activa.
     */
    @Override
    public EstadoEliminacionDTO obtenerEstado(String nombreUsuario) {
        UsuarioAuth usuarioAuth = usuarioAuthRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado"));

        return solicitudEliminacionRepository
                .findByUsuarioIdAndEstadoIn(usuarioAuth.getId(), ESTADOS_ACTIVOS)
                .map(solicitud -> {
                    Long diasRestantes = null;
                    if (solicitud.getEstado() == EstadoSolicitudEliminacion.PROGRAMADA
                            && solicitud.getFechaProgramadaEliminacion() != null) {
                        diasRestantes = java.time.Duration
                                .between(LocalDateTime.now(), solicitud.getFechaProgramadaEliminacion())
                                .toDays();
                        if (diasRestantes < 0) {
                            diasRestantes = 0L;
                        }
                    }

                    return EstadoEliminacionDTO.builder()
                            .tieneSolicitudActiva(true)
                            .estado(solicitud.getEstado().name())
                            .motivo(solicitud.getMotivo())
                            .comentario(solicitud.getComentario())
                            .fechaSolicitud(solicitud.getFechaSolicitud())
                            .fechaConfirmacion(solicitud.getFechaConfirmacion())
                            .fechaProgramadaEliminacion(solicitud.getFechaProgramadaEliminacion())
                            .diasRestantes(diasRestantes)
                            .build();
                })
                .orElse(EstadoEliminacionDTO.builder()
                        .tieneSolicitudActiva(false)
                        .build());
    }

    /**
     * Job automático que corre cada hora: busca las solicitudes cuyo plazo
     * de 30 días ya venció y anonimiza los datos personales del usuario,
     * conservando estadísticas generales (puntos, nivel, calificaciones,
     * spots creados, etc. no se tocan).
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void procesarEliminacionesVencidas() {
        List<SolicitudEliminacionCuenta> vencidas = solicitudEliminacionRepository
                .findByEstadoAndFechaProgramadaEliminacionBefore(
                        EstadoSolicitudEliminacion.PROGRAMADA, LocalDateTime.now());

        if (vencidas.isEmpty()) {
            return;
        }

        log.info("Procesando {} eliminaciones de cuenta vencidas", vencidas.size());

        for (SolicitudEliminacionCuenta solicitud : vencidas) {
            try {
                anonimizarCuenta(solicitud);
            } catch (Exception e) {
                log.error("Error anonimizando la cuenta {}: {}", solicitud.getUsuarioId(), e.getMessage(), e);
            }
        }
    }

    private void anonimizarCuenta(SolicitudEliminacionCuenta solicitud) {
        UsuarioAuth usuarioAuth = usuarioAuthRepository.findById(solicitud.getUsuarioId()).orElse(null);
        Usuario perfil = usuarioRepository.findById(solicitud.getUsuarioId()).orElse(null);

        if (usuarioAuth == null || perfil == null) {
            log.warn("No se encontró el usuario {} para anonimizar, se marca la solicitud como completada",
                    solicitud.getUsuarioId());
            solicitud.setEstado(EstadoSolicitudEliminacion.COMPLETADA);
            solicitud.setFechaCompletada(LocalDateTime.now());
            solicitudEliminacionRepository.save(solicitud);
            return;
        }

        String emailOriginal = usuarioAuth.getEmail();
        String sufijo = solicitud.getUsuarioId().toHexString();

        // Anonimizar datos personales identificables, sin tocar estadísticas
        // generales (puntos, nivel, calificaciones y spots ya creados quedan igual)
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

        solicitud.setEstado(EstadoSolicitudEliminacion.COMPLETADA);
        solicitud.setFechaCompletada(LocalDateTime.now());
        solicitudEliminacionRepository.save(solicitud);

        String htmlContent = emailService.construirHtmlEliminacionCompletada(solicitud.getNombreUsuario());
        emailService.enviarCorreoHtml(emailOriginal, "Tu cuenta fue eliminada - PhotoBogota", htmlContent);

        log.info("Cuenta {} anonimizada exitosamente", solicitud.getUsuarioId());
    }
}
