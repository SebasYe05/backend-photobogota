package com.photobogota.api.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.photobogota.api.dto.CrearPromocionRequestDTO;
import com.photobogota.api.dto.PromocionResponseDTO;
import com.photobogota.api.exception.AccessForbiddenException;
import com.photobogota.api.exception.OperacionInvalidaException;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.mapper.PromocionMapper;
import com.photobogota.api.model.Promocion;
import com.photobogota.api.model.Spot;
import com.photobogota.api.repository.PromocionRepository;
import com.photobogota.api.repository.SpotRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromocionServiceTest {

    @Mock
    private PromocionRepository promocionRepository;

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private PromocionMapper promocionMapper;

    @Mock
    private IFiltroContenidoService filtroContenidoService;

    @InjectMocks
    private PromocionService promocionService;

    private Spot localDeSocio() {
        Spot spot = new Spot();
        spot.setId("spot-1");
        spot.setNombre("Caldos Doña Gloria");
        spot.setCreadorRol("SOCIO");
        spot.setCreadorUsername("socio1");
        return spot;
    }

    private Promocion promocionVigente(String id) {
        return Promocion.builder()
                .id(id)
                .spotId("spot-1")
                .socioUsername("socio1")
                .titulo("2x1 en caldos")
                .descripcion("Descuento válido todos los días")
                .fechaInicio(LocalDate.now().minusDays(2).atStartOfDay())
                .fechaFin(LocalDate.now().plusDays(5).atTime(LocalTime.MAX))
                .activo(true)
                .usos(0)
                .usosMaximos(100)
                .build();
    }

    private Promocion promocionExpirada(String id) {
        Promocion promocion = promocionVigente(id);
        promocion.setFechaInicio(LocalDate.now().minusDays(30).atStartOfDay());
        promocion.setFechaFin(LocalDate.now().minusDays(1).atTime(LocalTime.MAX));
        return promocion;
    }

    private CrearPromocionRequestDTO requestDeEjemplo() {
        CrearPromocionRequestDTO request = new CrearPromocionRequestDTO();
        request.setSpotId("spot-1");
        request.setTitulo("2x1 en caldos");
        request.setDescripcion("Descuento válido todos los días");
        request.setTipo("2x1");
        request.setDescuento("50%");
        request.setCodigo("CALDO2X1");
        request.setImagenes(List.of("/promociones/promo.jpg"));
        request.setFechaInicio(LocalDate.now().minusDays(2).toString());
        request.setFechaFin(LocalDate.now().plusDays(5).toString());
        request.setUsosMaximos(100);
        return request;
    }

    private PromocionResponseDTO dtoDeEjemplo() {
        PromocionResponseDTO dto = new PromocionResponseDTO();
        dto.setId("prom-1");
        dto.setTitulo("2x1 en caldos");
        dto.setActivo(true);
        return dto;
    }

    @Test
    void crearPromocion_socioDueñoDelLocal_guardaYmapea() {
        when(spotRepository.findById("spot-1")).thenReturn(Optional.of(localDeSocio()));
        Promocion guardada = promocionVigente("prom-1");
        when(promocionRepository.save(any(Promocion.class))).thenReturn(guardada);
        when(promocionMapper.toResponse(guardada)).thenReturn(dtoDeEjemplo());

        PromocionResponseDTO resultado = promocionService.crearPromocion(requestDeEjemplo(), "socio1");

        assertThat(resultado.getTitulo()).isEqualTo("2x1 en caldos");
        verify(filtroContenidoService).verificarPermisoPublicar("socio1");
        verify(promocionRepository).save(any(Promocion.class));
    }

    @Test
    void crearPromocion_localQueNoEsDeSocio_lanzaOperacionInvalida() {
        Spot spot = localDeSocio();
        spot.setCreadorRol("MOD");
        when(spotRepository.findById("spot-1")).thenReturn(Optional.of(spot));

        assertThatThrownBy(() -> promocionService.crearPromocion(requestDeEjemplo(), "socio1"))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("Solo se pueden crear promociones para locales de socios");
        verify(promocionRepository, never()).save(any());
    }

    @Test
    void crearPromocion_otroSocio_lanzaAccessForbidden() {
        Spot spot = localDeSocio();
        spot.setCreadorUsername("socio2");
        when(spotRepository.findById("spot-1")).thenReturn(Optional.of(spot));

        assertThatThrownBy(() -> promocionService.crearPromocion(requestDeEjemplo(), "socio1"))
                .isInstanceOf(AccessForbiddenException.class);
    }

    @Test
    void crearPromocion_spotInexistente_lanzaResourceNotFound() {
        when(spotRepository.findById("spot-x")).thenReturn(Optional.empty());

        CrearPromocionRequestDTO request = requestDeEjemplo();
        request.setSpotId("spot-x");

        assertThatThrownBy(() -> promocionService.crearPromocion(request, "socio1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crearPromocion_fechaFinAntesDeInicio_lanzaOperacionInvalida() {
        when(spotRepository.findById("spot-1")).thenReturn(Optional.of(localDeSocio()));
        CrearPromocionRequestDTO request = requestDeEjemplo();
        request.setFechaInicio(LocalDate.now().plusDays(5).toString());
        request.setFechaFin(LocalDate.now().minusDays(2).toString());

        assertThatThrownBy(() -> promocionService.crearPromocion(request, "socio1"))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void actualizarPromocion_propia_actualizaTituloYDescripcion() {
        Promocion promocion = promocionVigente("prom-1");
        when(promocionRepository.findById("prom-1")).thenReturn(Optional.of(promocion));
        when(promocionRepository.save(promocion)).thenReturn(promocion);
        when(promocionMapper.toResponse(promocion)).thenReturn(dtoDeEjemplo());

        CrearPromocionRequestDTO request = requestDeEjemplo();
        request.setTitulo("3x2 en desayunos");

        PromocionResponseDTO resultado = promocionService.actualizarPromocion("prom-1", request, "socio1");

        assertThat(resultado.getTitulo()).isEqualTo("2x1 en caldos");
        assertThat(promocion.getTitulo()).isEqualTo("3x2 en desayunos");
    }

    @Test
    void actualizarPromocion_deOtroSocio_lanzaAccessForbidden() {
        Promocion promocion = promocionVigente("prom-1");
        promocion.setSocioUsername("socio2");
        when(promocionRepository.findById("prom-1")).thenReturn(Optional.of(promocion));

        assertThatThrownBy(() -> promocionService.actualizarPromocion("prom-1", requestDeEjemplo(), "socio1"))
                .isInstanceOf(AccessForbiddenException.class);
    }

    @Test
    void actualizarPromocion_inexistente_lanzaResourceNotFound() {
        when(promocionRepository.findById("prom-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promocionService.actualizarPromocion("prom-x", requestDeEjemplo(), "socio1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarMias_devuelveLasDelSocio() {
        when(promocionRepository.findBySocioUsername("socio1")).thenReturn(List.of(promocionVigente("prom-1")));
        when(promocionMapper.toResponse(any(Promocion.class))).thenReturn(dtoDeEjemplo());

        List<PromocionResponseDTO> resultado = promocionService.listarMias("socio1");

        assertThat(resultado).hasSize(1);
    }

    @Test
    void obtenerPorId_existente_devuelveDto() {
        when(promocionRepository.findById("prom-1")).thenReturn(Optional.of(promocionVigente("prom-1")));
        when(promocionMapper.toResponse(any(Promocion.class))).thenReturn(dtoDeEjemplo());

        PromocionResponseDTO resultado = promocionService.obtenerPorId("prom-1");

        assertThat(resultado.getId()).isEqualTo("prom-1");
    }

    @Test
    void obtenerPorId_inexistente_lanzaResourceNotFound() {
        when(promocionRepository.findById("prom-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promocionService.obtenerPorId("prom-x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarDeSpot_devuelveLasDelLocal() {
        when(promocionRepository.findBySpotId("spot-1")).thenReturn(List.of(promocionVigente("prom-1")));
        when(promocionMapper.toResponse(any(Promocion.class))).thenReturn(dtoDeEjemplo());

        List<PromocionResponseDTO> resultado = promocionService.listarDeSpot("spot-1");

        assertThat(resultado).hasSize(1);
    }

    @Test
    void obtenerActivaDeSpot_conVariasVigentes_devuelveLaMasReciente() {
        Promocion antigua = promocionVigente("prom-1");
        Promocion reciente = promocionVigente("prom-2");
        antigua.setFechaInicio(LocalDate.now().minusDays(4).atStartOfDay());
        antigua.setFechaFin(LocalDate.now().minusDays(1).atTime(LocalTime.MAX));
        when(promocionRepository.findBySpotId("spot-1")).thenReturn(List.of(reciente, antigua));
        when(promocionMapper.toResponse(reciente)).thenReturn(dtoDeEjemplo());

        PromocionResponseDTO resultado = promocionService.obtenerActivaDeSpot("spot-1");

        assertThat(resultado.getTitulo()).isEqualTo("2x1 en caldos");
    }

    @Test
    void obtenerActivaDeSpot_sinVigentes_lanzaResourceNotFound() {
        when(promocionRepository.findBySpotId("spot-1")).thenReturn(List.of(promocionExpirada("prom-1")));

        assertThatThrownBy(() -> promocionService.obtenerActivaDeSpot("spot-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no tiene una promoción activa");
    }

    @Test
    void listarActivas_filtraSoloLasVigentes() {
        when(promocionRepository.findByActivoTrue())
                .thenReturn(List.of(promocionVigente("prom-1"), promocionExpirada("prom-2")));
        when(promocionMapper.toResponse(any(Promocion.class))).thenReturn(dtoDeEjemplo());

        List<PromocionResponseDTO> resultado = promocionService.listarActivas();

        assertThat(resultado).hasSize(1);
    }

    @Test
    void obtenerSpotIdsConPromocionActiva_devuelveSoloLocalConVigente() {
        Promocion vigente = promocionVigente("prom-1");
        Promocion expirada = promocionExpirada("prom-2");
        expirada.setSpotId("spot-2");
        when(promocionRepository.findByActivoTrue()).thenReturn(List.of(vigente, expirada));

        Set<String> resultado = promocionService.obtenerSpotIdsConPromocionActiva();

        assertThat(resultado).containsExactly("spot-1");
    }

    @Test
    void tienePromocionActiva_conVigente_devuelveTrue() {
        when(promocionRepository.findBySpotId("spot-1")).thenReturn(List.of(promocionVigente("prom-1")));

        assertThat(promocionService.tienePromocionActiva("spot-1")).isTrue();
    }

    @Test
    void tienePromocionActiva_soloExpiradas_devuelveFalse() {
        when(promocionRepository.findBySpotId("spot-1")).thenReturn(List.of(promocionExpirada("prom-1")));

        assertThat(promocionService.tienePromocionActiva("spot-1")).isFalse();
    }

    @Test
    void desactivarPromocion_propia_invierteElEstado() {
        Promocion promocion = promocionVigente("prom-1");
        when(promocionRepository.findById("prom-1")).thenReturn(Optional.of(promocion));
        when(promocionRepository.save(promocion)).thenReturn(promocion);
        when(promocionMapper.toResponse(promocion)).thenReturn(dtoDeEjemplo());

        promocionService.desactivarPromocion("prom-1", "socio1");

        assertThat(promocion.getActivo()).isFalse();
    }

    @Test
    void duplicarPromocion_creaCopiaInactivaConUsoEnCero() {
        Promocion origen = promocionVigente("prom-1");
        when(promocionRepository.findById("prom-1")).thenReturn(Optional.of(origen));
        when(promocionRepository.save(any(Promocion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(promocionMapper.toResponse(any(Promocion.class))).thenReturn(dtoDeEjemplo());

        promocionService.duplicarPromocion("prom-1", "socio1");

        org.mockito.ArgumentCaptor<Promocion> captor = org.mockito.ArgumentCaptor.forClass(Promocion.class);
        verify(promocionRepository).save(captor.capture());
        assertThat(captor.getValue().getActivo()).isFalse();
        assertThat(captor.getValue().getUsos()).isEqualTo(0);
        assertThat(captor.getValue().getTitulo()).isEqualTo("2x1 en caldos");
    }

    @Test
    void eliminarPromocion_propia_eliminaElRegistro() {
        Promocion promocion = promocionVigente("prom-1");
        when(promocionRepository.findById("prom-1")).thenReturn(Optional.of(promocion));

        promocionService.eliminarPromocion("prom-1", "socio1");

        verify(promocionRepository).delete(promocion);
    }
}