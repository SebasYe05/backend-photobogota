package com.photobogota.api.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.photobogota.api.dto.EnviarNotificacionRequestDTO;
import com.photobogota.api.dto.EstadoMantenimientoDTO;
import com.photobogota.api.dto.MantenimientoResponseDTO;
import com.photobogota.api.dto.ProgramarMantenimientoRequestDTO;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.model.AlcanceNotificacion;
import com.photobogota.api.model.MantenimientoProgramado;
import com.photobogota.api.model.NotificacionTipo;
import com.photobogota.api.repository.MantenimientoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de mantenimiento programado (HU #47).
 *
 * Importante: NO reimplementa el envío de notificaciones. Reutiliza
 * INotificacionService.enviarNotificacionManual(...), el mismo método que ya
 * usa el Admin/Moderador para mandar cualquier otro anuncio
 * (POST /api/v1/notificaciones/enviar). Este servicio solo arma el
 * EnviarNotificacionRequestDTO con tipo=SISTEMA y alcance=TODOS y se lo pasa,
 * así que ambos flujos comparten el mismo pipeline de envío/preferencias/email.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MantenimientoServiceImpl implements IMantenimientoService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MantenimientoRepository mantenimientoRepository;
    private final INotificacionService notificacionService;

    @Override
    public MantenimientoResponseDTO programar(ProgramarMantenimientoRequestDTO request, String adminUsername) {
        if (!request.getFechaFin().isAfter(request.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
        }

        MantenimientoProgramado mantenimiento = MantenimientoProgramado.builder()
                .fechaInicio(request.getFechaInicio())
                .fechaFin(request.getFechaFin())
                .motivo(request.getMotivo())
                .mensaje(construirMensaje(request))
                .creadoPor(adminUsername)
                .build();

        mantenimiento = mantenimientoRepository.save(mantenimiento);

        enviarAviso("Mantenimiento programado", mantenimiento.getMensaje(), adminUsername);

        log.info("Mantenimiento programado por {} de {} a {}", adminUsername,
                mantenimiento.getFechaInicio(), mantenimiento.getFechaFin());

        return mapearAResponse(mantenimiento);
    }

    @Override
    public void cancelar(String id, String adminUsername) {
        MantenimientoProgramado mantenimiento = mantenimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mantenimiento no encontrado"));

        if (Boolean.TRUE.equals(mantenimiento.getCancelado())) {
            throw new IllegalArgumentException("Este mantenimiento ya estaba cancelado");
        }

        mantenimiento.setCancelado(true);
        mantenimientoRepository.save(mantenimiento);

        enviarAviso("Mantenimiento cancelado",
                "El mantenimiento programado para el " + mantenimiento.getFechaInicio().format(FORMATO_FECHA)
                        + " fue cancelado. El servicio continúa disponible con normalidad.",
                adminUsername);

        log.info("Mantenimiento {} cancelado por {}", id, adminUsername);
    }

    @Override
    public EstadoMantenimientoDTO obtenerEstado() {
        LocalDateTime ahora = LocalDateTime.now();

        return mantenimientoRepository
                .findFirstByCanceladoFalseAndFechaInicioBeforeAndFechaFinAfterOrderByFechaInicioDesc(ahora, ahora)
                .map(activo -> EstadoMantenimientoDTO.builder()
                        .enMantenimiento(true)
                        .fechaInicio(activo.getFechaInicio())
                        .fechaFin(activo.getFechaFin())
                        .mensaje(activo.getMensaje())
                        .build())
                .orElseGet(() -> construirEstadoSinMantenimientoActivo(ahora));
    }

    @Override
    public List<MantenimientoResponseDTO> listarProgramados() {
        return mantenimientoRepository.findByCanceladoFalseOrderByFechaInicioDesc().stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Override
    public void revisarYNotificarCambiosDeEstado() {
        LocalDateTime ahora = LocalDateTime.now();

        // Ventanas que ya iniciaron: solo se marca la bandera (el bloqueo real lo hace
        // el filtro comparando fechas en cada petición, esto evita reprocesarlas).
        List<MantenimientoProgramado> porIniciar = mantenimientoRepository
                .findByCanceladoFalseAndAvisoInicioEnviadoFalseAndFechaInicioBefore(ahora);
        for (MantenimientoProgramado m : porIniciar) {
            m.setAvisoInicioEnviado(true);
            mantenimientoRepository.save(m);
            log.info("Ventana de mantenimiento {} entró en vigor", m.getId());
        }

        // Ventanas que ya terminaron: se avisa que el servicio volvió a la normalidad.
        List<MantenimientoProgramado> porTerminar = mantenimientoRepository
                .findByCanceladoFalseAndAvisoFinEnviadoFalseAndFechaFinBefore(ahora);
        for (MantenimientoProgramado m : porTerminar) {
            m.setAvisoFinEnviado(true);
            mantenimientoRepository.save(m);
            enviarAviso("Mantenimiento finalizado",
                    "El mantenimiento programado ha finalizado. El servicio ya está disponible con normalidad.",
                    "sistema");
            log.info("Ventana de mantenimiento {} finalizada, servicio restablecido", m.getId());
        }
    }

    private EstadoMantenimientoDTO construirEstadoSinMantenimientoActivo(LocalDateTime ahora) {
        EstadoMantenimientoDTO.EstadoMantenimientoDTOBuilder builder = EstadoMantenimientoDTO.builder()
                .enMantenimiento(false);

        mantenimientoRepository.findFirstByCanceladoFalseAndFechaInicioAfterOrderByFechaInicioAsc(ahora)
                .ifPresent(proximo -> builder
                        .proximoInicio(proximo.getFechaInicio())
                        .proximoFin(proximo.getFechaFin()));

        return builder.build();
    }

    private String construirMensaje(ProgramarMantenimientoRequestDTO request) {
        if (request.getMensajePersonalizado() != null && !request.getMensajePersonalizado().isBlank()) {
            return request.getMensajePersonalizado();
        }
        return "El servidor entrará en mantenimiento (" + request.getMotivo() + ") de "
                + request.getFechaInicio().format(FORMATO_FECHA) + " a "
                + request.getFechaFin().format(FORMATO_FECHA)
                + ". Durante ese periodo la plataforma no estará disponible.";
    }

    // Reutiliza el servicio genérico de notificaciones: mismo pipeline que usa
    // el Admin/Moderador para enviar cualquier otro anuncio (ANUNCIO_ADMIN, etc.)
    private void enviarAviso(String titulo, String mensaje, String emisorUsername) {
        EnviarNotificacionRequestDTO aviso = EnviarNotificacionRequestDTO.builder()
                .titulo(titulo)
                .mensaje(mensaje)
                .tipo(NotificacionTipo.SISTEMA)
                .alcance(AlcanceNotificacion.TODOS)
                .build();

        notificacionService.enviarNotificacionManual(aviso, emisorUsername, "ADMIN");
    }

    private MantenimientoResponseDTO mapearAResponse(MantenimientoProgramado m) {
        return MantenimientoResponseDTO.builder()
                .id(m.getId())
                .fechaInicio(m.getFechaInicio())
                .fechaFin(m.getFechaFin())
                .mensaje(m.getMensaje())
                .motivo(m.getMotivo())
                .creadoPor(m.getCreadoPor())
                .cancelado(m.getCancelado())
                .fechaCreacion(m.getFechaCreacion())
                .build();
    }
}
