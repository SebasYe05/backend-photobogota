package com.photobogota.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.photobogota.api.dto.EstadoMantenimientoDTO;
import com.photobogota.api.dto.EnviarNotificacionRequestDTO;
import com.photobogota.api.dto.MantenimientoResponseDTO;
import com.photobogota.api.dto.ProgramarMantenimientoRequestDTO;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.model.AlcanceNotificacion;
import com.photobogota.api.model.MantenimientoProgramado;
import com.photobogota.api.model.NotificacionTipo;
import com.photobogota.api.repository.MantenimientoRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MantenimientoServiceImplTest {

    @Mock
    private MantenimientoRepository mantenimientoRepository;

    @Mock
    private INotificacionService notificacionService;

    @InjectMocks
    private MantenimientoServiceImpl mantenimientoService;

    private ProgramarMantenimientoRequestDTO requestDeEjemplo() {
        ProgramarMantenimientoRequestDTO request = new ProgramarMantenimientoRequestDTO();
        request.setFechaInicio(LocalDateTime.of(2030, 1, 10, 2, 0));
        request.setFechaFin(LocalDateTime.of(2030, 1, 10, 4, 0));
        request.setMotivo("Actualización de plataforma");
        return request;
    }

    private MantenimientoProgramado mantenimientoProgramado() {
        return MantenimientoProgramado.builder()
                .id("mant-1")
                .fechaInicio(LocalDateTime.of(2030, 1, 10, 2, 0))
                .fechaFin(LocalDateTime.of(2030, 1, 10, 4, 0))
                .motivo("Actualización de plataforma")
                .mensaje("El servidor entrará en mantenimiento")
                .creadoPor("admin")
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    @Test
    void programar_rangoValido_guardaYnotifica() {
        MantenimientoProgramado programado = mantenimientoProgramado();
        when(mantenimientoRepository.save(any(MantenimientoProgramado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MantenimientoResponseDTO resultado = mantenimientoService.programar(requestDeEjemplo(), "admin1");

        assertThat(resultado.getMotivo()).isEqualTo("Actualización de plataforma");
        assertThat(resultado.getCreadoPor()).isEqualTo("admin1");
        verify(mantenimientoRepository).save(any(MantenimientoProgramado.class));
        verify(notificacionService).enviarNotificacionManual(any(EnviarNotificacionRequestDTO.class), eq("admin1"), eq("ADMIN"));
    }

    @Test
    void programar_fechaFinIgualAInicio_lanzaExcepcion() {
        ProgramarMantenimientoRequestDTO request = requestDeEjemplo();
        request.setFechaFin(request.getFechaInicio());

        assertThatThrownBy(() -> mantenimientoService.programar(request, "admin1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posterior a la fecha de inicio");
        verify(mantenimientoRepository, never()).save(any());
    }

    @Test
    void programar_mensajePersonalizado_seRespeta() {
        ProgramarMantenimientoRequestDTO request = requestDeEjemplo();
        request.setMensajePersonalizado("Ventana de mantenimiento nocturna");
        MantenimientoProgramado programado = mantenimientoProgramado();
        programado.setMensaje("Ventana de mantenimiento nocturna");
        when(mantenimientoRepository.save(any(MantenimientoProgramado.class))).thenReturn(programado);

        MantenimientoResponseDTO resultado = mantenimientoService.programar(request, "admin1");

        assertThat(resultado.getMensaje()).isEqualTo("Ventana de mantenimiento nocturna");
    }

    @Test
    void cancelar_existente_marcaCanceladoYnotifica() {
        MantenimientoProgramado mantenimiento = mantenimientoProgramado();
        when(mantenimientoRepository.findById("mant-1")).thenReturn(Optional.of(mantenimiento));

        mantenimientoService.cancelar("mant-1", "admin1");

        assertThat(mantenimiento.getCancelado()).isTrue();
        verify(mantenimientoRepository).save(mantenimiento);
        verify(notificacionService).enviarNotificacionManual(any(EnviarNotificacionRequestDTO.class), eq("admin1"), eq("ADMIN"));
    }

    @Test
    void cancelar_yaCancelado_lanzaExcepcionYnoGuarda() {
        MantenimientoProgramado mantenimiento = mantenimientoProgramado();
        mantenimiento.setCancelado(true);
        when(mantenimientoRepository.findById("mant-1")).thenReturn(Optional.of(mantenimiento));

        assertThatThrownBy(() -> mantenimientoService.cancelar("mant-1", "admin1"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(mantenimientoRepository, never()).save(any());
    }

    @Test
    void cancelar_inexistente_lanzaResourceNotFound() {
        when(mantenimientoRepository.findById("mant-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mantenimientoService.cancelar("mant-x", "admin1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerEstado_mantenimientoEnCurso_reportaActivo() {
        MantenimientoProgramado activo = mantenimientoProgramado();
        when(mantenimientoRepository.findFirstByCanceladoFalseAndFechaInicioBeforeAndFechaFinAfterOrderByFechaInicioDesc(
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(Optional.of(activo));

        EstadoMantenimientoDTO estado = mantenimientoService.obtenerEstado();

        assertThat(estado.isEnMantenimiento()).isTrue();
        assertThat(estado.getMensaje()).isEqualTo(activo.getMensaje());
    }

    @Test
    void obtenerEstado_sinActivo_implicaNoEstarEnMantenimiento() {
        when(mantenimientoRepository.findFirstByCanceladoFalseAndFechaInicioBeforeAndFechaFinAfterOrderByFechaInicioDesc(
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(Optional.empty());
        when(mantenimientoRepository.findFirstByCanceladoFalseAndFechaInicioAfterOrderByFechaInicioAsc(any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        EstadoMantenimientoDTO estado = mantenimientoService.obtenerEstado();

        assertThat(estado.isEnMantenimiento()).isFalse();
        assertThat(estado.getProximoInicio()).isNull();
    }

    @Test
    void obtenerEstado_sinActivoConProximo_informaProximoInicio() {
        when(mantenimientoRepository.findFirstByCanceladoFalseAndFechaInicioBeforeAndFechaFinAfterOrderByFechaInicioDesc(
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(Optional.empty());
        when(mantenimientoRepository.findFirstByCanceladoFalseAndFechaInicioAfterOrderByFechaInicioAsc(any(LocalDateTime.class)))
                .thenReturn(Optional.of(mantenimientoProgramado()));

        EstadoMantenimientoDTO estado = mantenimientoService.obtenerEstado();

        assertThat(estado.isEnMantenimiento()).isFalse();
        assertThat(estado.getProximoInicio()).isEqualTo(LocalDateTime.of(2030, 1, 10, 2, 0));
    }

    @Test
    void listarProgramados_devuelveTodosLosRegistrados() {
        when(mantenimientoRepository.findAllByOrderByFechaInicioDesc()).thenReturn(
                List.of(mantenimientoProgramado()));

        List<MantenimientoResponseDTO> resultado = mantenimientoService.listarProgramados();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo("mant-1");
        assertThat(resultado.get(0).getCreadoPor()).isEqualTo("admin");
    }

    @Test
    void revisarYNotificarCambiosDeEstado_marcaBanderaYavisaFin() {
        MantenimientoProgramado porIniciar = mantenimientoProgramado();
        porIniciar.setId("mant-inicio");
        MantenimientoProgramado porTerminar = mantenimientoProgramado();
        porTerminar.setId("mant-fin");
        when(mantenimientoRepository.findByCanceladoFalseAndAvisoInicioEnviadoFalseAndFechaInicioBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(porIniciar));
        when(mantenimientoRepository.findByCanceladoFalseAndAvisoFinEnviadoFalseAndFechaFinBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(porTerminar));

        mantenimientoService.revisarYNotificarCambiosDeEstado();

        assertThat(porIniciar.getAvisoInicioEnviado()).isTrue();
        assertThat(porTerminar.getAvisoFinEnviado()).isTrue();
        verify(mantenimientoRepository, times(2)).save(any(MantenimientoProgramado.class));
        verify(notificacionService).enviarNotificacionManual(any(EnviarNotificacionRequestDTO.class), eq("sistema"), eq("ADMIN"));
    }
}