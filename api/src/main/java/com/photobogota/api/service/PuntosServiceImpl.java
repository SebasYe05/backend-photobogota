package com.photobogota.api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.photobogota.api.dto.PuntosResponseDTO;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.model.HistorialPuntos;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.PuntosConfig;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.TipoPuntos;
import com.photobogota.api.model.Usuario;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.HistorialPuntosRepository;
import com.photobogota.api.repository.MiembroRepository;
import com.photobogota.api.repository.PuntosConfigRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PuntosServiceImpl implements IPuntosService {

    private static final String TZ_DEFAULT = "America/Bogota";

    private final MiembroRepository miembroRepository;
    private final UsuarioAuthRepository usuarioAuthRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialPuntosRepository historialPuntosRepository;
    private final PuntosConfigRepository puntosConfigRepository;
    private final INotificacionService notificacionService;

    @Override
    public int sumarPuntos(String nombreUsuario, TipoPuntos tipo, String refId) {
        if (nombreUsuario == null || tipo == null || refId == null) {
            return 0;
        }

        UsuarioAuth auth = usuarioAuthRepository.findByNombreUsuario(nombreUsuario).orElse(null);
        if (auth == null || auth.getRol() != Rol.MIEMBRO) {
            return 0;
        }

        Usuario usuario = usuarioRepository.findById(auth.getId()).orElse(null);
        if (!(usuario instanceof Miembro miembro)) {
            return 0;
        }

        if (historialPuntosRepository.existsByUsuarioAndTipoAndRefId(nombreUsuario, tipo, refId)) {
            return 0;
        }

        int puntosAAwardear = obtenerValorConfig(claveDeTipo(tipo), valorDefaultDeTipo(tipo));

        if (tipo != TipoPuntos.AJUSTE_ADMIN) {
            int limiteDiario = obtenerLimiteDiario();
            int puntosHoy = calcularPuntosHoy(nombreUsuario);
            int disponible = limiteDiario - puntosHoy;
            if (disponible <= 0) {
                log.info("Usuario {} alcanzó límite diario de puntos ({}). No se suman puntos para acción {}",
                        nombreUsuario, limiteDiario, tipo);
                return 0;
            }
            puntosAAwardear = Math.min(puntosAAwardear, disponible);
            if (puntosAAwardear <= 0) {
                return 0;
            }
        }

        int nivelAnterior = miembro.getNivel() != null ? miembro.getNivel() : 1;
        long puntosActuales = miembro.getPuntos() != null ? miembro.getPuntos() : 0L;
        miembro.setPuntos(puntosActuales + puntosAAwardear);
        int nivelNuevo = calcularNivel(miembro.getPuntos());
        miembro.setNivel(nivelNuevo);
        miembroRepository.save(miembro);

        HistorialPuntos historial = HistorialPuntos.builder()
                .usuario(nombreUsuario)
                .tipo(tipo)
                .puntos(puntosAAwardear)
                .refId(refId)
                .fecha(LocalDateTime.now())
                .motivo(null)
                .build();
        historialPuntosRepository.save(historial);

        if (nivelNuevo > nivelAnterior) {
            try {
                notificacionService.notificarSubidaNivel(nombreUsuario, nivelNuevo);
            } catch (Exception e) {
                log.error("No se pudo notificar la subida de nivel de {} a nivel {}: {}",
                        nombreUsuario, nivelNuevo, e.getMessage());
            }
        }

        return puntosAAwardear;
    }

    @Override
    public PuntosResponseDTO obtenerPuntos(String nombreUsuario) {
        if (nombreUsuario == null) {
            return null;
        }

        UsuarioAuth auth = usuarioAuthRepository.findByNombreUsuario(nombreUsuario).orElse(null);
        if (auth == null || auth.getRol() != Rol.MIEMBRO) {
            return PuntosResponseDTO.builder()
                    .puntos(0L)
                    .nivel(1)
                    .puntosHoy(0)
                    .limiteDiario(obtenerLimiteDiario())
                    .puntosParaSiguienteNivel(obtenerBaseUmbral())
                    .progresoPercent(0)
                    .build();
        }

        Usuario usuario = usuarioRepository.findById(auth.getId()).orElse(null);
        if (!(usuario instanceof Miembro miembro)) {
            return PuntosResponseDTO.builder()
                    .puntos(0L)
                    .nivel(1)
                    .puntosHoy(0)
                    .limiteDiario(obtenerLimiteDiario())
                    .puntosParaSiguienteNivel(obtenerBaseUmbral())
                    .progresoPercent(0)
                    .build();
        }

        long puntos = miembro.getPuntos() != null ? miembro.getPuntos() : 0L;
        int nivel = miembro.getNivel() != null ? miembro.getNivel() : 1;
        int puntosHoy = calcularPuntosHoy(nombreUsuario);
        int limiteDiario = obtenerLimiteDiario();

        long umbralNivelActual = obtenerUmbralNivel(nivel);
        long umbralSiguienteNivel = obtenerUmbralNivel(nivel + 1);
        long puntosParaSiguiente = Math.max(0, umbralSiguienteNivel - puntos);

        // Progreso dentro del tramo del nivel actual: [umbralNivelActual,
        // umbralSiguienteNivel).
        // Antes había un caso especial para nivel == 1 que dividía por
        // umbralNivelActual
        // (que para nivel 1 siempre es 0, por definición de obtenerUmbralNivel), así
        // que
        // esa rama devolvía 0 SIEMPRE sin importar los puntos reales: root cause del
        // bug
        // reportado por QA (10 puntos -> progresoPercent 0 en vez de ~10). La fórmula
        // general de abajo ya es correcta también para nivel 1 (umbralNivelActual = 0),
        // así que se eliminó el caso especial.
        int progresoPercent;
        long deltaNivel = umbralSiguienteNivel - umbralNivelActual;
        if (deltaNivel <= 0) {
            progresoPercent = 100;
        } else {
            progresoPercent = (int) Math.min(100,
                    Math.max(0, ((puntos - umbralNivelActual) * 100) / deltaNivel));
        }

        return PuntosResponseDTO.builder()
                .puntos(puntos)
                .nivel(nivel)
                .puntosHoy(puntosHoy)
                .limiteDiario(limiteDiario)
                .puntosParaSiguienteNivel(puntosParaSiguiente)
                .progresoPercent(progresoPercent)
                .build();
    }

    @Override
    public Map<String, String> obtenerConfig() {
        List<PuntosConfig> configs = puntosConfigRepository.findAll();
        return configs.stream()
                .collect(Collectors.toMap(c -> c.getClave(), c -> c.getValor()));
    }

    @Override
    public Map<String, String> actualizarConfig(Map<String, String> nuevaConfig) {
        List<PuntosConfig> existentes = puntosConfigRepository.findAll();
        Map<String, PuntosConfig> porClave = existentes.stream()
                .collect(Collectors.toMap(c -> c.getClave(), c -> c));

        List<PuntosConfig> guardar = nuevaConfig.entrySet().stream()
                .map(entry -> {
                    PuntosConfig config = porClave.get(entry.getKey());
                    if (config != null) {
                        config.setValor(entry.getValue());
                        return config;
                    }
                    return PuntosConfig.builder()
                            .clave(entry.getKey())
                            .valor(entry.getValue())
                            .build();
                })
                .collect(Collectors.toList());

        puntosConfigRepository.saveAll(guardar);
        return obtenerConfig();
    }

    @Override
    public PuntosResponseDTO ajustarPuntosPorId(String usuarioId, int delta, String motivo) {
        if (usuarioId == null || delta == 0) {
            throw new IllegalArgumentException("UsuarioId y delta son requeridos");
        }

        Usuario usuario = usuarioRepository.findById(new org.bson.types.ObjectId(usuarioId))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));

        if (!(usuario instanceof Miembro miembro)) {
            throw new ResourceNotFoundException("El usuario no es un MIEMBRO y no tiene puntos");
        }

        String nombreUsuario = usuarioAuthRepository.findById(new org.bson.types.ObjectId(usuarioId))
                .orElseThrow(() -> new ResourceNotFoundException("UsuarioAuth no encontrado"))
                .getNombreUsuario();

        int nivelAnterior = miembro.getNivel() != null ? miembro.getNivel() : 1;
        long puntosActuales = miembro.getPuntos() != null ? miembro.getPuntos() : 0L;
        long nuevosPuntos = puntosActuales + delta;
        if (nuevosPuntos < 0) {
            nuevosPuntos = 0;
        }

        miembro.setPuntos(nuevosPuntos);
        int nivelNuevo = calcularNivel(nuevosPuntos);
        miembro.setNivel(nivelNuevo);
        miembroRepository.save(miembro);

        HistorialPuntos historial = HistorialPuntos.builder()
                .usuario(nombreUsuario)
                .tipo(TipoPuntos.AJUSTE_ADMIN)
                .puntos(delta)
                .refId("admin-ajuste-" + usuarioId)
                .fecha(LocalDateTime.now())
                .motivo(motivo)
                .build();
        historialPuntosRepository.save(historial);

        if (nivelNuevo > nivelAnterior) {
            try {
                notificacionService.notificarSubidaNivel(nombreUsuario, nivelNuevo);
            } catch (Exception e) {
                log.error("No se pudo notificar la subida de nivel de {} a nivel {}: {}",
                        nombreUsuario, nivelNuevo, e.getMessage());
            }
        }

        return obtenerPuntos(nombreUsuario);
    }

    private String claveDeTipo(TipoPuntos tipo) {
        return switch (tipo) {
            case CREAR_SPOT -> "puntos.crear_spot";
            case CALIFICAR_SPOT -> "puntos.calificar_spot";
            case REPORTE_VALIDADO -> "puntos.reporte_validado";
            case SUGERIR_SPOT_APROBADO -> "puntos.sugerir_spot_aprobado";
            case AJUSTE_ADMIN -> "puntos.ajuste_admin";
        };
    }

    private int valorDefaultDeTipo(TipoPuntos tipo) {
        return switch (tipo) {
            case CREAR_SPOT -> 10;
            case CALIFICAR_SPOT -> 3;
            case REPORTE_VALIDADO -> 15;
            case SUGERIR_SPOT_APROBADO -> 25;
            case AJUSTE_ADMIN -> 0;
        };
    }

    private int obtenerValorConfig(String clave, int defecto) {
        return puntosConfigRepository.findById(clave)
                .map(c -> {
                    try {
                        return Integer.parseInt(c.getValor());
                    } catch (NumberFormatException e) {
                        return defecto;
                    }
                })
                .orElse(defecto);
    }

    private int obtenerLimiteDiario() {
        return obtenerValorConfig("puntos.limite_diario", 100);
    }

    private long obtenerBaseUmbral() {
        return obtenerValorConfig("puntos.base_umbral", 100);
    }

    private int calcularPuntosHoy(String nombreUsuario) {
        LocalDateTime inicioHoy = getInicioHoy();
        List<HistorialPuntos> historial = historialPuntosRepository
                .findByUsuarioAndFechaGreaterThanEqual(nombreUsuario, inicioHoy);
        return historial.stream()
                .mapToInt(h -> h.getPuntos())
                .sum();
    }

    private LocalDateTime getInicioHoy() {
        String timezone = "America/Bogota";
        try {
            timezone = puntosConfigRepository.findById("puntos.timezone")
                    .map(config -> config.getValor()) // Lambda simple
                    .orElse("America/Bogota");
        } catch (Exception e) {
            log.warn("Error al leer timezone de puntos, usando default {}", TZ_DEFAULT);
        }

        try {
            ZoneId zone = ZoneId.of(timezone);
            LocalDate hoy = LocalDate.now(zone);
            return LocalDateTime.of(hoy, LocalTime.of(0, 0));
        } catch (Exception e) {
            log.warn("Timezone inválida '{}', usando default {}", timezone, TZ_DEFAULT);
            ZoneId zone = ZoneId.of(TZ_DEFAULT);
            LocalDate hoy = LocalDate.now(zone);
            return LocalDateTime.of(hoy, LocalTime.of(0, 0));
        }
    }

    private int calcularNivel(long puntosTotales) {
        if (puntosTotales <= 0) {
            return 1;
        }

        int nivel = 1;
        long umbral = 0;

        while (true) {
            long siguienteUmbral;
            if (nivel == 1) {
                siguienteUmbral = obtenerBaseUmbral();
            } else {
                siguienteUmbral = (long) Math.floor(umbral * 1.5);
            }

            if (puntosTotales < siguienteUmbral) {
                return nivel;
            }

            umbral = siguienteUmbral;
            nivel++;
        }
    }

    private long obtenerUmbralNivel(int nivelObjetivo) {
        if (nivelObjetivo <= 1) {
            return 0;
        }

        long umbral = 0;
        for (int i = 2; i <= nivelObjetivo; i++) {
            if (i == 2) {
                umbral = obtenerBaseUmbral();
            } else {
                umbral = (long) Math.floor(umbral * 1.5);
            }
        }
        return umbral;
    }
}
