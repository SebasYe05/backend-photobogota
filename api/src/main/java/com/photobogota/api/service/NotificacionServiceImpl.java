package com.photobogota.api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.photobogota.api.dto.EnviarNotificacionRequestDTO;
import com.photobogota.api.dto.NotificacionResponseDTO;
import com.photobogota.api.dto.PreferenciasNotificacionDTO;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.mapper.NotificacionMapper;
import com.photobogota.api.model.CanalNotificacion;
import com.photobogota.api.model.Notificacion;
import com.photobogota.api.model.NotificacionTipo;
import com.photobogota.api.model.PreferenciasNotificacion;
import com.photobogota.api.model.Spot;
import com.photobogota.api.repository.NotificacionRepository;
import com.photobogota.api.repository.PreferenciasNotificacionRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionServiceImpl implements INotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final PreferenciasNotificacionRepository preferenciasNotificacionRepository;
    private final UsuarioAuthRepository usuarioAuthRepository;
    private final NotificacionMapper notificacionMapper;
    private final IEmailService emailService;

    // ==================== BANDEJA DEL USUARIO ====================

    @Override
    public Page<NotificacionResponseDTO> listarMisNotificaciones(String username, Pageable pageable,
            Boolean soloNoLeidas) {
        Page<Notificacion> pagina = Boolean.TRUE.equals(soloNoLeidas)
                ? notificacionRepository.findByDestinatarioUsernameAndLeidaOrderByFechaCreacionDesc(username, false,
                        pageable)
                : notificacionRepository.findByDestinatarioUsernameOrderByFechaCreacionDesc(username, pageable);

        return pagina.map(notificacionMapper::toResponse);
    }

    @Override
    public long contarNoLeidas(String username) {
        return notificacionRepository.countByDestinatarioUsernameAndLeidaFalse(username);
    }

    @Override
    public void marcarLeida(String id, String username) {
        Notificacion notificacion = obtenerPropia(id, username);
        if (!Boolean.TRUE.equals(notificacion.getLeida())) {
            notificacion.setLeida(true);
            notificacionRepository.save(notificacion);
        }
    }

    @Override
    public void marcarTodasLeidas(String username) {
        List<Notificacion> pendientes = notificacionRepository.findByDestinatarioUsernameAndLeidaFalse(username);
        pendientes.forEach(n -> n.setLeida(true));
        notificacionRepository.saveAll(pendientes);
    }

    @Override
    public void eliminarNotificacion(String id, String username) {
        Notificacion notificacion = obtenerPropia(id, username);
        notificacionRepository.delete(notificacion);
    }

    private Notificacion obtenerPropia(String id, String username) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));
        if (!notificacion.getDestinatarioUsername().equals(username)) {
            // No revelamos si existe o no; para el usuario ajeno es como si no existiera.
            throw new ResourceNotFoundException("Notificación no encontrada");
        }
        return notificacion;
    }

    // ==================== PREFERENCIAS ====================

    @Override
    public PreferenciasNotificacionDTO obtenerPreferencias(String username) {
        PreferenciasNotificacion prefs = obtenerPreferenciasEfectivas(username);
        return mapearADto(prefs);
    }

    @Override
    public PreferenciasNotificacionDTO actualizarPreferencias(String username, PreferenciasNotificacionDTO dto) {
        PreferenciasNotificacion prefs = preferenciasNotificacionRepository.findByUsername(username)
                .orElseGet(() -> PreferenciasNotificacion.builder().username(username).build());

        if (dto.getNotificacionesActivas() != null) {
            prefs.setNotificacionesActivas(dto.getNotificacionesActivas());
        }
        if (dto.getCanalPreferido() != null) {
            prefs.setCanalPreferido(dto.getCanalPreferido());
        }
        if (dto.getTiposSilenciados() != null) {
            prefs.setTiposSilenciados(dto.getTiposSilenciados());
        }
        if (dto.getLocalidadesInteres() != null) {
            prefs.setLocalidadesInteres(dto.getLocalidadesInteres());
        }
        if (dto.getCategoriasInteres() != null) {
            prefs.setCategoriasInteres(dto.getCategoriasInteres());
        }

        PreferenciasNotificacion guardado = preferenciasNotificacionRepository.save(prefs);
        log.info("Preferencias de notificaciones actualizadas para {}", username);
        return mapearADto(guardado);
    }

    private PreferenciasNotificacion obtenerPreferenciasEfectivas(String username) {
        return preferenciasNotificacionRepository.findByUsername(username)
                .orElseGet(() -> PreferenciasNotificacion.builder().username(username).build());
    }

    private PreferenciasNotificacionDTO mapearADto(PreferenciasNotificacion prefs) {
        return PreferenciasNotificacionDTO.builder()
                .notificacionesActivas(prefs.getNotificacionesActivas())
                .canalPreferido(prefs.getCanalPreferido())
                .tiposSilenciados(prefs.getTiposSilenciados())
                .localidadesInteres(prefs.getLocalidadesInteres())
                .categoriasInteres(prefs.getCategoriasInteres())
                .build();
    }

    // ==================== ENVÍO MANUAL (ADMIN / MODERADOR) ====================

    @Override
    public void enviarNotificacionManual(EnviarNotificacionRequestDTO request, String emisorUsername,
            String emisorRol) {
        NotificacionTipo tipoFinal = request.getTipo() != null
                ? request.getTipo()
                : ("MOD".equalsIgnoreCase(emisorRol) ? NotificacionTipo.ANUNCIO_MODERADOR
                        : NotificacionTipo.ANUNCIO_ADMIN);

        List<String> destinatarios = resolverDestinatarios(request);

        for (String destinatario : destinatarios) {
            if (destinatario.equals(emisorUsername)) {
                continue; // no notificarse a sí mismo
            }
            crearYEnviar(obtenerPreferenciasEfectivas(destinatario), tipoFinal, request.getTitulo(),
                    request.getMensaje(), null, emisorUsername);
        }

        log.info("Notificación manual ({}) enviada por {} a {} destinatario(s)", tipoFinal, emisorUsername,
                destinatarios.size());
    }

    private List<String> resolverDestinatarios(EnviarNotificacionRequestDTO request) {
        switch (request.getAlcance()) {
            case TODOS:
                return usuarioAuthRepository.findAll().stream()
                        .filter(java.util.Objects::nonNull)
                        .map(u -> u.getNombreUsuario())
                        .toList();

            case POR_ROL:
                if (request.getRoles() == null || request.getRoles().isEmpty()) {
                    throw new IllegalArgumentException("Debes indicar al menos un rol para este alcance");
                }
                return request.getRoles().stream()
                        .filter(java.util.Objects::nonNull)
                        .flatMap(rol -> usuarioAuthRepository.findByRol(rol).stream())
                        .map(u -> u.getNombreUsuario())
                        .distinct()
                        .toList();

            case USUARIOS_ESPECIFICOS:
            default:
                if (request.getUsernames() == null || request.getUsernames().isEmpty()) {
                    throw new IllegalArgumentException("Debes indicar al menos un usuario destinatario");
                }
                return request.getUsernames().stream()
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList();
        }
    }
    // ==================== DISPARADORES AUTOMÁTICOS ====================

    @Override
    public void notificarNuevoSpot(Spot spot) {
        List<PreferenciasNotificacion> interesados = preferenciasNotificacionRepository
                .findByLocalidadesInteresContainingOrCategoriasInteresContaining(spot.getLocalidad(),
                        spot.getCategoria());

        String titulo = "Nuevo spot en " + spot.getLocalidad();
        String mensaje = "Se publicó \"" + spot.getNombre() + "\" en " + spot.getLocalidad()
                + " (" + spot.getCategoria() + "). ¡Échale un vistazo!";

        for (PreferenciasNotificacion prefs : interesados) {
            if (prefs.getUsername().equals(spot.getCreadorUsername())) {
                continue; // no notificar al propio creador del spot
            }
            crearYEnviar(prefs, NotificacionTipo.NUEVO_SPOT_INTERES, titulo, mensaje, spot.getId(), "sistema");
        }
    }

    @Override
    public void notificarNuevaResena(Spot spot, Spot.Resena resena, String usuarioQueResenio) {
        String destinatario = spot.getCreadorUsername();
        if (destinatario == null || destinatario.equals(usuarioQueResenio)) {
            return; // no notificar auto-reseñas ni spots sin dueño
        }

        String titulo = "Nueva reseña en tu spot";
        String mensaje = usuarioQueResenio + " calificó \"" + spot.getNombre() + "\" con "
                + resena.getRating() + " estrella(s).";

        crearYEnviar(obtenerPreferenciasEfectivas(destinatario), NotificacionTipo.NUEVA_RESENA, titulo, mensaje,
                spot.getId(), usuarioQueResenio);
    }

    // ==================== NÚCLEO COMÚN: crear + enviar respetando preferencias
    // ====================

    private void crearYEnviar(PreferenciasNotificacion prefs, NotificacionTipo tipo, String titulo, String mensaje,
            String spotId, String emisorUsername) {

        if (!Boolean.TRUE.equals(prefs.getNotificacionesActivas())) {
            return; // el usuario desactivó las notificaciones (criterio de aceptación #4)
        }
        if (prefs.getTiposSilenciados() != null && prefs.getTiposSilenciados().contains(tipo)) {
            return; // el usuario silenció este tipo específico
        }

        Notificacion notificacion = Notificacion.builder()
                .destinatarioUsername(prefs.getUsername())
                .tipo(tipo)
                .titulo(titulo)
                .mensaje(mensaje)
                .spotId(spotId)
                .emisorUsername(emisorUsername)
                .leida(false)
                .fechaCreacion(LocalDateTime.now())
                .build();

        notificacionRepository.save(notificacion);

        CanalNotificacion canal = prefs.getCanalPreferido() != null ? prefs.getCanalPreferido()
                : CanalNotificacion.APP;

        if (canal == CanalNotificacion.EMAIL || canal == CanalNotificacion.AMBOS) {
            enviarCorreoNotificacion(prefs.getUsername(), titulo, mensaje);
        }
    }

    private void enviarCorreoNotificacion(String destinatarioUsername, String titulo, String mensaje) {
        usuarioAuthRepository.findByNombreUsuario(destinatarioUsername).ifPresent(auth -> {
            try {
                String html = "<div style=\"font-family:Arial,sans-serif;\">"
                        + "<h2>" + titulo + "</h2>"
                        + "<p>" + mensaje + "</p>"
                        + "<p style=\"color:#888;font-size:12px;\">PhotoBogotá</p>"
                        + "</div>";
                emailService.enviarCorreoHtml(auth.getEmail(), titulo, html);
            } catch (Exception e) {
                log.error("No se pudo enviar el correo de notificación a {}: {}", destinatarioUsername,
                        e.getMessage());
            }
        });
    }
}
