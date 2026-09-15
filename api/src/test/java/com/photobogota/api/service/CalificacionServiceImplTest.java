package com.photobogota.api.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.photobogota.api.dto.CalificacionRequestDTO;
import com.photobogota.api.dto.CalificacionResponseDTO;
import com.photobogota.api.exception.ResourceAlreadyExistsException;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.model.Calificacion;
import com.photobogota.api.model.Spot;
import com.photobogota.api.model.TipoContenidoModerado;
import com.photobogota.api.repository.CalificacionRepository;
import com.photobogota.api.repository.SpotRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalificacionServiceImplTest {

    @Mock
    private CalificacionRepository calificacionRepository;

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private INotificacionService notificacionService;

    @Mock
    private IPuntosService puntosService;

    @Mock
    private IFiltroContenidoService filtroContenidoService;

    @InjectMocks
    private CalificacionServiceImpl calificacionService;

    private Spot spotDeEjemplo() {
        Spot spot = new Spot();
        spot.setId("spot-1");
        spot.setNombre("Parque Simón Bolívar");
        return spot;
    }

    private Calificacion calificacionDeEjemplo() {
        return Calificacion.builder()
                .spotId("spot-1")
                .usuario("miembro1")
                .estrellas(5)
                .comentario("Excelente lugar para fotos")
                .build();
    }

    private CalificacionRequestDTO requestDeEjemplo() {
        CalificacionRequestDTO request = new CalificacionRequestDTO();
        request.setEstrellas(4);
        request.setComentario("Buena iluminación al atardecer");
        return request;
    }

    @Test
    void crearCalificacion_validaPersisteRecalculaNotificayOtorgaPuntos() {
        Spot spot = spotDeEjemplo();
        when(spotRepository.findById("spot-1")).thenReturn(Optional.of(spot));
        when(calificacionRepository.findBySpotIdAndUsuario("spot-1", "miembro1")).thenReturn(null);
        when(calificacionRepository.findBySpotId("spot-1")).thenReturn(List.of(calificacionDeEjemplo()));

        CalificacionResponseDTO resultado = calificacionService.crearCalificacion(
                "spot-1", requestDeEjemplo(), "miembro1");

        assertThat(resultado.getUsuario()).isEqualTo("miembro1");
        assertThat(resultado.getRating()).isEqualTo(4);
        assertThat(resultado.getComentario()).isEqualTo("Buena iluminación al atardecer");
        assertThat(spot.getRating()).isEqualTo(5.0);
        assertThat(spot.getTotalResenas()).isEqualTo(1);
        verify(calificacionRepository).save(any(Calificacion.class));
        verify(spotRepository).save(spot);
        verify(filtroContenidoService).validarContenido(eq("miembro1"), eq(TipoContenidoModerado.RESENA), any());
        verify(notificacionService).notificarNuevaCalificacion(any(Spot.class), any(Calificacion.class), eq("miembro1"));
        verify(puntosService).sumarPuntos(eq("miembro1"), any(), any());
    }

    @Test
    void crearCalificacion_usuarioYaCalifico_lanzaResourceAlreadyExists() {
        when(spotRepository.findById("spot-1")).thenReturn(Optional.of(spotDeEjemplo()));
        when(calificacionRepository.findBySpotIdAndUsuario("spot-1", "miembro1")).thenReturn(calificacionDeEjemplo());

        assertThatThrownBy(() -> calificacionService.crearCalificacion("spot-1", requestDeEjemplo(), "miembro1"))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(calificacionRepository, never()).save(any());
    }

    @Test
    void crearCalificacion_spotInexistente_lanzaResourceNotFound() {
        when(spotRepository.findById("spot-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calificacionService.crearCalificacion("spot-x", requestDeEjemplo(), "miembro1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void modificarCalificacion_exitosa_actualizaYRecalcula() {
        Calificacion calificacion = calificacionDeEjemplo();
        Spot spot = spotDeEjemplo();
        when(calificacionRepository.findById("cal-1")).thenReturn(Optional.of(calificacion));
        when(spotRepository.findById("spot-1")).thenReturn(Optional.of(spot));
        when(calificacionRepository.findBySpotId("spot-1")).thenReturn(List.of(calificacion));

        CalificacionResponseDTO resultado = calificacionService.modificarCalificacion(
                "spot-1", "cal-1", requestDeEjemplo(), "miembro1");

        assertThat(resultado.getRating()).isEqualTo(4);
        assertThat(spot.getRating()).isEqualTo(4.0);
        verify(calificacionRepository).save(calificacion);
    }

    @Test
    void modificarCalificacion_deOtroUsuario_lanzaResourceNotFound() {
        Calificacion calificacion = calificacionDeEjemplo();
        calificacion.setUsuario("otroUsuario");
        when(calificacionRepository.findById("cal-1")).thenReturn(Optional.of(calificacion));

        assertThatThrownBy(() -> calificacionService.modificarCalificacion(
                "spot-1", "cal-1", requestDeEjemplo(), "miembro1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No tienes permiso");
    }

    @Test
    void modificarCalificacion_deOtroSpot_lanzaResourceNotFound() {
        Calificacion calificacion = calificacionDeEjemplo();
        calificacion.setSpotId("spot-2");
        when(calificacionRepository.findById("cal-1")).thenReturn(Optional.of(calificacion));

        assertThatThrownBy(() -> calificacionService.modificarCalificacion(
                "spot-1", "cal-1", requestDeEjemplo(), "miembro1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void modificarCalificacion_inexistente_lanzaResourceNotFound() {
        when(calificacionRepository.findById("cal-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calificacionService.modificarCalificacion(
                "spot-1", "cal-x", requestDeEjemplo(), "miembro1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarPorSpot_devuelveLasCalificacionesDelSpot() {
        when(calificacionRepository.findBySpotId("spot-1")).thenReturn(List.of(calificacionDeEjemplo()));

        List<CalificacionResponseDTO> resultado = calificacionService.listarPorSpot("spot-1");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstrellas()).isEqualTo(5);
    }

    @Test
    void obtenerPorId_existente_devuelveDto() {
        when(calificacionRepository.findById("cal-1")).thenReturn(Optional.of(calificacionDeEjemplo()));

        CalificacionResponseDTO resultado = calificacionService.obtenerPorId("cal-1");

        assertThat(resultado.getComentario()).isEqualTo("Excelente lugar para fotos");
    }

    @Test
    void obtenerPorId_inexistente_lanzaResourceNotFound() {
        when(calificacionRepository.findById("cal-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calificacionService.obtenerPorId("cal-x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}