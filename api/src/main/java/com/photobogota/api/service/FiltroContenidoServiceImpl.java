package com.photobogota.api.service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.photobogota.api.dto.PalabraProhibidaDTO;
import com.photobogota.api.dto.RegistroModeracionDTO;
import com.photobogota.api.dto.ResolverApelacionRequestDTO;
import com.photobogota.api.dto.SancionDTO;
import com.photobogota.api.exception.ContenidoInapropiadoException;
import com.photobogota.api.exception.OperacionInvalidaException;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.model.AccionModeracion;
import com.photobogota.api.model.EstadoApelacion;
import com.photobogota.api.model.PalabraProhibida;
import com.photobogota.api.model.RegistroModeracion;
import com.photobogota.api.model.Sancion;
import com.photobogota.api.model.TipoContenidoModerado;
import com.photobogota.api.model.TipoPalabra;
import com.photobogota.api.model.TipoSancion;
import com.photobogota.api.model.Usuario;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.PalabraProhibidaRepository;
import com.photobogota.api.repository.RegistroModeracionRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FiltroContenidoServiceImpl implements IFiltroContenidoService {

    private static final String ORIGEN_AUTO = "AUTO";
    private static final long DIAS_MUTE = 5;
    private static final long DIAS_SUSPENSION = 3;

    private final PalabraProhibidaRepository palabraProhibidaRepository;
    private final RegistroModeracionRepository registroModeracionRepository;
    private final UsuarioAuthRepository usuarioAuthRepository;
    private final UsuarioRepository usuarioRepository;
    private final INotificacionService notificacionService;
    private final MongoTemplate mongoTemplate;

    // ==================== DETECCIÓN AUTOMÁTICA ====================

    @Override
    @Transactional
    public void verificarPermisoPublicar(String nombreUsuario) {
        Usuario usuario = obtenerUsuario(nombreUsuario);
        Sancion sancion = usuario.getSancion();
        if (sancion == null || sancion.getTipo() == TipoSancion.NOTIFICACION) {
            return;
        }

        boolean expirada = sancion.getFechaExpiracion() != null
                && sancion.getFechaExpiracion().isBefore(LocalDateTime.now());
        if (expirada) {
            usuario.setSancion(null);
            usuarioRepository.save(usuario);
            return;
        }

        String mensaje = switch (sancion.getTipo()) {
            case MUTE -> "No puedes publicar contenido: tienes un silencio activo hasta el "
                    + sancion.getFechaExpiracion() + ".";
            case SUSPENSION -> "No puedes publicar contenido: tu cuenta está suspendida hasta el "
                    + sancion.getFechaExpiracion() + ".";
            case BAN -> "No puedes publicar contenido: tu cuenta está suspendida indefinidamente.";
            default -> "No puedes publicar contenido en este momento.";
        };

        throw new ContenidoInapropiadoException(mensaje, List.of(), sancion.getTipo(),
                sancion.getFechaExpiracion(), usuario.getContadorInfracciones());
    }

    @Override
    @Transactional
    public void validarContenido(String nombreUsuario, TipoContenidoModerado tipo, String contenido) {
        verificarPermisoPublicar(nombreUsuario);

        List<String> palabrasDetectadas = analizar(contenido);
        if (palabrasDetectadas.isEmpty()) {
            return;
        }

        Usuario usuario = obtenerUsuario(nombreUsuario);
        int nuevoConteo = (usuario.getContadorInfracciones() == null ? 0 : usuario.getContadorInfracciones()) + 1;
        TipoSancion sancionTipo = calcularSancion(nuevoConteo);
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime expiracion = null;

        if (sancionTipo == TipoSancion.MUTE) {
            expiracion = ahora.plusDays(DIAS_MUTE);
        } else if (sancionTipo == TipoSancion.SUSPENSION) {
            expiracion = ahora.plusDays(DIAS_SUSPENSION);
        }

        Sancion nuevaSancion = Sancion.builder()
                .tipo(sancionTipo)
                .fechaInicio(ahora)
                .fechaExpiracion(expiracion)
                .motivo("Contenido inapropiado detectado automáticamente: " + String.join(", ", palabrasDetectadas))
                .impuestaPor(ORIGEN_AUTO)
                .build();

        usuario.setContadorInfracciones(nuevoConteo);
        usuario.setSancion(nuevaSancion);
        usuarioRepository.save(usuario);

        String usuarioId = usuario.getId().toHexString();
        registrar(usuarioId, nombreUsuario, AccionModeracion.DETECCION, tipo, contenido, palabrasDetectadas,
                "Contenido bloqueado por el filtro automático. Palabras detectadas: "
                        + String.join(", ", palabrasDetectadas),
                ORIGEN_AUTO, null);
        registrar(usuarioId, nombreUsuario, accionDeSanccion(sancionTipo), tipo, contenido, palabrasDetectadas,
                descripcionSanccion(sancionTipo, expiracion), ORIGEN_AUTO, null);

        notificarSanccion(nombreUsuario, sancionTipo, palabrasDetectadas, expiracion);

        String mensaje = "Tu contenido fue rechazado por contener lenguaje inapropiado: "
                + String.join(", ", palabrasDetectadas)
                + ". Se ha aplicado la sanción: " + etiquetaSanccion(sancionTipo) + ".";

        throw new ContenidoInapropiadoException(mensaje, palabrasDetectadas, sancionTipo, expiracion, nuevoConteo);
    }

    /**
     * Detecta las palabras/frases prohibidas presentes en el texto, con análisis
     * de contexto para reducir falsos positivos:
     * - Las PALABRAS solo cuentan como coincidencia de palabra completa (frontera
     *   de palabra) y sin distinción de tildes.
     * - Si la palabra aparece dentro de una "excepción" (frase de contexto
     *   permitida configurada), se descarta como falso positivo.
     * - Las FRASES cuentan como coincidencia de subcadena normalizada.
     */
    List<String> analizar(String texto) {
        List<String> detectadas = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return detectadas;
        }

        String textoNormalizado = normalizar(texto);

        for (PalabraProhibida regla : palabraProhibidaRepository.findByActivoTrue()) {
            String textoRegla = normalizar(regla.getTexto());
            if (textoRegla.isBlank()) {
                continue;
            }

            if (regla.getTipo() == TipoPalabra.FRASE) {
                if (textoNormalizado.contains(textoRegla)) {
                    detectadas.add(regla.getTexto());
                }
                continue;
            }

            Pattern pattern = Pattern.compile(
                    "(?<![\\p{L}\\p{N}])" + Pattern.quote(textoRegla) + "(?![\\p{L}\\p{N}])");
            Matcher matcher = pattern.matcher(textoNormalizado);
            List<int[]> rangosExcepciones = rangosDeExcepciones(regla, textoNormalizado);

            boolean infringe = false;
            while (matcher.find()) {
                if (!dentroDeExcepcion(matcher.start(), matcher.end(), rangosExcepciones)) {
                    infringe = true;
                    break;
                }
            }
            if (infringe) {
                detectadas.add(regla.getTexto());
            }
        }

        return detectadas;
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.replaceAll("\\s+", " ").trim();
    }

    private List<int[]> rangosDeExcepciones(PalabraProhibida regla, String textoNormalizado) {
        List<int[]> rangos = new ArrayList<>();
        if (regla.getExcepciones() == null) {
            return rangos;
        }
        for (String excepcion : regla.getExcepciones()) {
            String excepcionNormalizada = normalizar(excepcion);
            if (excepcionNormalizada.isBlank()) {
                continue;
            }
            int idx = textoNormalizado.indexOf(excepcionNormalizada);
            while (idx >= 0) {
                rangos.add(new int[] { idx, idx + excepcionNormalizada.length() });
                idx = textoNormalizado.indexOf(excepcionNormalizada, idx + excepcionNormalizada.length());
            }
        }
        return rangos;
    }

    private boolean dentroDeExcepcion(int start, int end, List<int[]> rangosExcepciones) {
        for (int[] rango : rangosExcepciones) {
            if (start >= rango[0] && end <= rango[1]) {
                return true;
            }
        }
        return false;
    }

    // ==================== SANCIONES PROGRESIVAS ====================

    private TipoSancion calcularSancion(int contador) {
        if (contador >= 4) {
            return TipoSancion.BAN;
        }
        if (contador == 3) {
            return TipoSancion.SUSPENSION;
        }
        if (contador == 2) {
            return TipoSancion.MUTE;
        }
        return TipoSancion.NOTIFICACION;
    }

    private AccionModeracion accionDeSanccion(TipoSancion tipo) {
        return switch (tipo) {
            case NOTIFICACION -> AccionModeracion.NOTIFICACION;
            case MUTE -> AccionModeracion.MUTE;
            case SUSPENSION -> AccionModeracion.SUSPENSION;
            case BAN -> AccionModeracion.BAN;
        };
    }

    private String descripcionSanccion(TipoSancion tipo, LocalDateTime expiracion) {
        return switch (tipo) {
            case NOTIFICACION -> "Primera infracción: notificación enviada al usuario.";
            case MUTE -> "Segunda infracción: silencio de " + DIAS_MUTE + " días (hasta " + expiracion + ").";
            case SUSPENSION -> "Tercera infracción: suspensión de " + DIAS_SUSPENSION + " días (hasta "
                    + expiracion + ").";
            case BAN -> "Cuarta infracción o más: suspensión indefinida. El usuario puede apelar.";
        };
    }

    private String etiquetaSanccion(TipoSancion tipo) {
        return switch (tipo) {
            case NOTIFICACION -> "notificación";
            case MUTE -> "silencio por " + DIAS_MUTE + " días";
            case SUSPENSION -> "suspensión por " + DIAS_SUSPENSION + " días";
            case BAN -> "suspensión indefinida";
        };
    }

    private void notificarSanccion(String nombreUsuario, TipoSancion tipo, List<String> palabras,
            LocalDateTime expiracion) {
        String titulo;
        String mensaje;
        switch (tipo) {
            case NOTIFICACION -> {
                titulo = "Aviso de moderación";
                mensaje = "Tu contenido fue rechazado por contener lenguaje inapropiado: "
                        + String.join(", ", palabras) + ". Recuerda las normas de la comunidad.";
            }
            case MUTE -> {
                titulo = "Silencio temporal";
                mensaje = "Tu contenido fue rechazado. Tu cuenta queda silenciada por " + DIAS_MUTE
                        + " días (hasta " + expiracion + "). Podrás volver a publicar después de esa fecha.";
            }
            case SUSPENSION -> {
                titulo = "Cuenta suspendida temporalmente";
                mensaje = "Tu cuenta queda suspendida por " + DIAS_SUSPENSION + " días (hasta " + expiracion
                        + ") por incumplir las normas de la comunidad.";
            }
            default -> {
                titulo = "Cuenta suspendida";
                mensaje = "Tu cuenta ha sido suspendida indefinidamente por incumplir las normas de la comunidad. "
                        + "Puedes enviar una apelación desde tu perfil.";
            }
        }
        notificacionService.notificarSistema(nombreUsuario, titulo, mensaje);
    }

    // ==================== CONSULTA DE SANCIÓN Y APELACIÓN ====================

    @Override
    public SancionDTO obtenerSancionActual(String nombreUsuario) {
        Usuario usuario = obtenerUsuario(nombreUsuario);
        return construirSancionDTO(usuario);
    }

    @Override
    @Transactional
    public void apelarBan(String nombreUsuario, String motivo) {
        Usuario usuario = obtenerUsuario(nombreUsuario);
        Sancion sancion = usuario.getSancion();

        if (sancion == null || sancion.getTipo() != TipoSancion.BAN) {
            throw new OperacionInvalidaException("Solo puedes apelar si tienes una suspensión indefinida activa");
        }

        RegistroModeracion registroBan = ultimoRegistroBanSinApelacion(usuario.getId().toHexString());
        if (registroBan == null) {
            throw new OperacionInvalidaException("No hay una suspensión pendiente de apelar");
        }

        registroBan.setEstadoApelacion(EstadoApelacion.PENDIENTE);
        registroBan.setMotivoApelacion(motivo);
        registroBan.setFechaApelacion(LocalDateTime.now());
        registroModeracionRepository.save(registroBan);

        registrar(usuario.getId().toHexString(), nombreUsuario, AccionModeracion.DETECCION, null, null, List.of(),
                "El usuario envió una apelación de su suspensión indefinida. Motivo: " + motivo, nombreUsuario, null);
    }

    private RegistroModeracion ultimoRegistroBanSinApelacion(String usuarioId) {
        List<RegistroModeracion> registros = registroModeracionRepository
                .findByUsuarioIdOrderByFechaDesc(usuarioId);
        return registros.stream()
                .filter(r -> r.getAccion() == AccionModeracion.BAN)
                .filter(r -> r.getEstadoApelacion() == null || r.getEstadoApelacion() == EstadoApelacion.PENDIENTE)
                .findFirst()
                .orElse(null);
    }

    // ==================== GESTIÓN DE PALABRAS PROHIBIDAS (ADMIN) ====================

    @Override
    public List<PalabraProhibidaDTO> listarPalabras() {
        return palabraProhibidaRepository.findAllByOrderByFechaCreacionDesc().stream()
                .map(this::mapearPalabraADTO)
                .toList();
    }

    @Override
    @Transactional
    public PalabraProhibidaDTO crearPalabra(PalabraProhibidaDTO dto, String adminUsername) {
        if (dto.getTexto() == null || dto.getTexto().isBlank()) {
            throw new OperacionInvalidaException("El texto de la palabra o frase es obligatorio");
        }

        PalabraProhibida regla = PalabraProhibida.builder()
                .texto(dto.getTexto().trim())
                .tipo(dto.getTipo() != null ? dto.getTipo() : TipoPalabra.PALABRA)
                .activo(dto.getActivo() != null ? dto.getActivo() : true)
                .categoria(dto.getCategoria())
                .excepciones(dto.getExcepciones() != null ? dto.getExcepciones() : new ArrayList<>())
                .creadoPor(adminUsername)
                .fechaCreacion(LocalDateTime.now())
                .build();

        return mapearPalabraADTO(palabraProhibidaRepository.save(regla));
    }

    @Override
    @Transactional
    public PalabraProhibidaDTO actualizarPalabra(String id, PalabraProhibidaDTO dto) {
        PalabraProhibida regla = palabraProhibidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Palabra prohibida no encontrada con id: " + id));

        if (dto.getTexto() != null && !dto.getTexto().isBlank()) {
            regla.setTexto(dto.getTexto().trim());
        }
        if (dto.getTipo() != null) {
            regla.setTipo(dto.getTipo());
        }
        if (dto.getActivo() != null) {
            regla.setActivo(dto.getActivo());
        }
        if (dto.getCategoria() != null) {
            regla.setCategoria(dto.getCategoria());
        }
        if (dto.getExcepciones() != null) {
            regla.setExcepciones(dto.getExcepciones());
        }

        return mapearPalabraADTO(palabraProhibidaRepository.save(regla));
    }

    @Override
    @Transactional
    public void eliminarPalabra(String id) {
        if (!palabraProhibidaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Palabra prohibida no encontrada con id: " + id);
        }
        palabraProhibidaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public PalabraProhibidaDTO togglePalabra(String id) {
        PalabraProhibida regla = palabraProhibidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Palabra prohibida no encontrada con id: " + id));
        regla.setActivo(!Boolean.TRUE.equals(regla.getActivo()));
        return mapearPalabraADTO(palabraProhibidaRepository.save(regla));
    }

    // ==================== HISTORIAL Y APELACIONES (ADMIN) ====================

    @Override
    public Page<RegistroModeracionDTO> listarHistorial(AccionModeracion accion, String nombreUsuario,
            TipoContenidoModerado tipoContenido, LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {

        Query query = new Query();
        if (accion != null) {
            query.addCriteria(Criteria.where(RegistroModeracion.Fields.accion).is(null));
        }
        if (nombreUsuario != null && !nombreUsuario.isBlank()) {
            query.addCriteria(Criteria.where(RegistroModeracion.Fields.nombreUsuario).regex(Pattern.quote(nombreUsuario), "i"));
        }
        if (tipoContenido != null) {
            query.addCriteria(Criteria.where(RegistroModeracion.Fields.tipoContenido).is(tipoContenido));
        }
        if (desde != null) {
            query.addCriteria(Criteria.where(RegistroModeracion.Fields.fecha).gte(desde));
        }
        if (hasta != null) {
            query.addCriteria(Criteria.where(RegistroModeracion.Fields.fecha).lte(hasta));
        }

        long total = mongoTemplate.count(query, RegistroModeracion.class);
        query.with(pageable);
        List<RegistroModeracion> registros = mongoTemplate.find(query, RegistroModeracion.class);

        return new PageImpl<>(
                registros.stream().map(this::mapearRegistroADTO).toList(),
                pageable,
                total);
    }

    @Override
    public List<RegistroModeracionDTO> listarApelacionesPendientes() {
        return registroModeracionRepository
                .findByAccionAndEstadoApelacionOrderByFechaDesc(AccionModeracion.BAN, EstadoApelacion.PENDIENTE)
                .stream()
                .map(this::mapearRegistroADTO)
                .toList();
    }

    @Override
    @Transactional
    public RegistroModeracionDTO resolverApelacion(String registroId, ResolverApelacionRequestDTO request,
            String adminUsername) {

        RegistroModeracion registro = registroModeracionRepository.findById(registroId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de moderación no encontrado"));

        if (registro.getAccion() != AccionModeracion.BAN
                || registro.getEstadoApelacion() != EstadoApelacion.PENDIENTE) {
            throw new OperacionInvalidaException("Este registro no tiene una apelación pendiente");
        }

        Usuario usuario = usuarioRepository.findById(new org.bson.types.ObjectId(registro.getUsuarioId()))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        boolean aprobar = Boolean.TRUE.equals(request.getAprobar());
        registro.setEstadoApelacion(aprobar ? EstadoApelacion.APROBADA : EstadoApelacion.RECHAZADA);
        registro.setRespuestaApelacion(request.getRespuesta());
        registro.setRevisadaPor(adminUsername);
        registro.setFechaRevision(LocalDateTime.now());
        registroModeracionRepository.save(registro);

        if (aprobar) {
            usuario.setSancion(null);
            usuarioRepository.save(usuario);
            registrar(registro.getUsuarioId(), registro.getNombreUsuario(), AccionModeracion.BAN_REVOCADO, null, null,
                    List.of(),
                    "Apelación aprobada: la suspensión indefinida fue levantada por " + adminUsername, adminUsername,
                    null);
            notificacionService.notificarSistema(registro.getNombreUsuario(), "Apelación aprobada",
                    "Tu suspensión fue levantada. Respuesta: " + request.getRespuesta());
        } else {
            registrar(registro.getUsuarioId(), registro.getNombreUsuario(), AccionModeracion.APELACION_RECHAZADA, null,
                    null, List.of(),
                    "Apelación rechazada por " + adminUsername + ". Respuesta: " + request.getRespuesta(),
                    adminUsername, null);
            notificacionService.notificarSistema(registro.getNombreUsuario(), "Apelación rechazada",
                    "Tu suspensión se mantiene. Respuesta: " + request.getRespuesta());
        }

        return mapearRegistroADTO(registro);
    }

    // ==================== HELPERS ====================

    private Usuario obtenerUsuario(String nombreUsuario) {
        UsuarioAuth auth = usuarioAuthRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return usuarioRepository.findById(auth.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de usuario no encontrado"));
    }

    private void registrar(String usuarioId, String nombreUsuario, AccionModeracion accion,
            TipoContenidoModerado tipo, String contenido, List<String> palabras, String detalle, String origen,
            EstadoApelacion estadoApelacion) {
        registroModeracionRepository.save(RegistroModeracion.builder()
                .usuarioId(usuarioId)
                .nombreUsuario(nombreUsuario)
                .accion(accion)
                .tipoContenido(tipo)
                .contenidoOriginal(contenido != null && contenido.length() > 500 ? contenido.substring(0, 500)
                        : contenido)
                .palabrasDetectadas(palabras != null ? palabras : new ArrayList<>())
                .detalle(detalle)
                .origen(origen)
                .fecha(LocalDateTime.now())
                .estadoApelacion(estadoApelacion)
                .build());
    }

    private SancionDTO construirSancionDTO(Usuario usuario) {
        Sancion sancion = usuario.getSancion();
        int contador = usuario.getContadorInfracciones() == null ? 0 : usuario.getContadorInfracciones();

        if (sancion == null) {
            return SancionDTO.builder()
                    .tipo(null)
                    .contadorInfracciones(contador)
                    .bloqueaPublicacion(false)
                    .puedeApelar(false)
                    .build();
        }

        boolean expirada = sancion.getFechaExpiracion() != null
                && sancion.getFechaExpiracion().isBefore(LocalDateTime.now());
        boolean bloquea = !expirada && (sancion.getTipo() == TipoSancion.MUTE
                || sancion.getTipo() == TipoSancion.SUSPENSION
                || sancion.getTipo() == TipoSancion.BAN);
        boolean puedeApelar = !expirada && sancion.getTipo() == TipoSancion.BAN;

        return SancionDTO.builder()
                .tipo(expirada ? null : sancion.getTipo())
                .fechaInicio(sancion.getFechaInicio())
                .fechaExpiracion(sancion.getFechaExpiracion())
                .motivo(sancion.getMotivo())
                .contadorInfracciones(contador)
                .bloqueaPublicacion(bloquea)
                .puedeApelar(puedeApelar)
                .build();
    }

    private PalabraProhibidaDTO mapearPalabraADTO(PalabraProhibida regla) {
        return PalabraProhibidaDTO.builder()
                .id(regla.getId())
                .texto(regla.getTexto())
                .tipo(regla.getTipo())
                .activo(regla.getActivo())
                .categoria(regla.getCategoria())
                .excepciones(regla.getExcepciones())
                .creadoPor(regla.getCreadoPor())
                .fechaCreacion(regla.getFechaCreacion())
                .build();
    }

    private RegistroModeracionDTO mapearRegistroADTO(RegistroModeracion registro) {
        return RegistroModeracionDTO.builder()
                .id(registro.getId())
                .usuarioId(registro.getUsuarioId())
                .nombreUsuario(registro.getNombreUsuario())
                .accion(registro.getAccion())
                .tipoContenido(registro.getTipoContenido())
                .contenidoOriginal(registro.getContenidoOriginal())
                .palabrasDetectadas(registro.getPalabrasDetectadas())
                .detalle(registro.getDetalle())
                .origen(registro.getOrigen())
                .fecha(registro.getFecha())
                .estadoApelacion(registro.getEstadoApelacion())
                .motivoApelacion(registro.getMotivoApelacion())
                .fechaApelacion(registro.getFechaApelacion())
                .respuestaApelacion(registro.getRespuestaApelacion())
                .revisadaPor(registro.getRevisadaPor())
                .fechaRevision(registro.getFechaRevision())
                .build();
    }
}
