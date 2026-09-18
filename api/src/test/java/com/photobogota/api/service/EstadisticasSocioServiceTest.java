package com.photobogota.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.photobogota.api.dto.EstadisticasSocioDTO;
import com.photobogota.api.model.Canje;
import com.photobogota.api.model.Spot;
import com.photobogota.api.model.VistaSpot;
import com.photobogota.api.repository.CanjeRepository;
import com.photobogota.api.repository.PromocionRepository;
import com.photobogota.api.repository.SpotRepository;
import com.photobogota.api.repository.VistaSpotRepository;

@ExtendWith(MockitoExtension.class)
class EstadisticasSocioServiceTest {

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private VistaSpotRepository vistaSpotRepository;

    @Mock
    private CanjeRepository canjeRepository;

    @Mock
    private PromocionRepository promocionRepository;

    @InjectMocks
    private EstadisticasSocioService servicio;

    private Spot localConResenas(String id, LocalDateTime... fechasResenas) {
        Spot spot = new Spot();
        spot.setId(id);
        spot.setNombre("Local " + id);
        spot.setTipo("LOCAL");
        spot.setCreadorUsername("socio1");
        spot.setCreadorRol("SOCIO");
        spot.setImagenes(List.of("/spots/" + id + ".jpg"));
        int i = 1;
        for (LocalDateTime fecha : fechasResenas) {
            Spot.Resena resena = new Spot.Resena();
            resena.setUsuario("miembro" + i);
            resena.setRating(i);
            resena.setFecha(fecha);
            spot.getResenas().add(resena);
            i++;
        }
        return spot;
    }

    @Test
    void obtenerEstadisticas_sinLocales_devuelveTambienKpisCeroYSeriesVacias() {
        when(spotRepository.findByCreadorUsername("socio1")).thenReturn(List.of());
        when(promocionRepository.findBySocioUsername("socio1")).thenReturn(List.of());

        EstadisticasSocioDTO dto = servicio.obtenerEstadisticas("socio1", "mes");

        assertThat(dto.getPeriodo()).isEqualTo("mes");
        assertThat(dto.getKpis().getVisitas()).isZero();
        assertThat(dto.getKpis().getResenas()).isZero();
        assertThat(dto.getKpis().getCalificacionPromedio()).isZero();
        assertThat(dto.getKpis().getUsosPromociones()).isZero();
        assertThat(dto.getSeriesVisitas()).isNotEmpty();
        assertThat(dto.getDistribucionResenas()).hasSize(5);
        assertThat(dto.getLugaresPopulares()).isEmpty();
        assertThat(dto.isHayPromociones()).isFalse();
    }

    @Test
    void obtenerEstadisticas_conDatos_agregaVisitasResenasYUsos() {
        Spot local = localConResenas("s1", LocalDateTime.now().minusDays(1), LocalDateTime.now().minusHours(2));
        local.setRating(4.0);
        when(spotRepository.findByCreadorUsername("socio1")).thenReturn(List.of(local));

        VistaSpot v1 = VistaSpot.builder().spotId("s1").usuario("miembro1").fecha(LocalDateTime.now().minusDays(2)).build();
        VistaSpot v2 = VistaSpot.builder().spotId("s1").usuario("miembro2").fecha(LocalDateTime.now().minusHours(5)).build();
        when(vistaSpotRepository.findBySpotIdInAndFechaBetween(any(), any(), any())).thenReturn(List.of(v1, v2));

        Canje canje = Canje.builder().socioUsername("socio1").fechaCanje(LocalDateTime.now().minusDays(1)).build();
        when(canjeRepository.findBySocioUsernameAndFechaCanjeBetween(eq("socio1"), any(), any()))
                .thenReturn(List.of(canje));

        when(promocionRepository.findBySocioUsername("socio1")).thenReturn(List.of(new com.photobogota.api.model.Promocion()));

        EstadisticasSocioDTO dto = servicio.obtenerEstadisticas("socio1", "mes");

        assertThat(dto.getKpis().getVisitas()).isEqualTo(2);
        assertThat(dto.getKpis().getResenas()).isEqualTo(2);
        assertThat(dto.getKpis().getCalificacionPromedio()).isEqualTo(1.5);
        assertThat(dto.getKpis().getUsosPromociones()).isEqualTo(1);
        assertThat(dto.isHayPromociones()).isTrue();
        assertThat(dto.getDistribucionResenas()).hasSize(5);
        assertThat(dto.getSeriesVisitas().stream().mapToLong(p -> p.getValor()).sum()).isEqualTo(2);
        assertThat(dto.getSeriesUsosPromociones().stream().mapToLong(p -> p.getValor()).sum()).isEqualTo(1);
        assertThat(dto.getLugaresPopulares()).hasSize(1);
        assertThat(dto.getLugaresPopulares().get(0).getNombre()).isEqualTo("Local s1");
        assertThat(dto.getLugaresPopulares().get(0).getVisitas()).isEqualTo(2);
        assertThat(dto.getLugaresPopulares().get(0).getImagen()).isEqualTo("/spots/s1.jpg");
    }

    @Test
    void obtenerEstadisticas_periodoSemana_normalizaEtiquetas() {
        when(spotRepository.findByCreadorUsername("socio1")).thenReturn(List.of());
        when(promocionRepository.findBySocioUsername("socio1")).thenReturn(List.of());

        EstadisticasSocioDTO dto = servicio.obtenerEstadisticas("socio1", "semana");

        assertThat(dto.getPeriodo()).isEqualTo("semana");
        assertThat(dto.getSeriesVisitas()).hasSize(7);
    }

    @Test
    void obtenerEstadisticas_periodoAno_normalizaEtiquetas() {
        when(spotRepository.findByCreadorUsername("socio1")).thenReturn(List.of());
        when(promocionRepository.findBySocioUsername("socio1")).thenReturn(List.of());

        EstadisticasSocioDTO dto = servicio.obtenerEstadisticas("socio1", "ano");

        assertThat(dto.getPeriodo()).isEqualTo("ano");
        assertThat(dto.getSeriesVisitas()).hasSize(12);
    }
}