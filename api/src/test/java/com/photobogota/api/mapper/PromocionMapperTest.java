package com.photobogota.api.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.photobogota.api.dto.PromocionResponseDTO;
import com.photobogota.api.model.Promocion;

import static org.assertj.core.api.Assertions.assertThat;

class PromocionMapperTest {

    private final PromocionMapper mapper = new PromocionMapperImpl();

    private Promocion promocionActiva() {
        return Promocion.builder()
                .id("p1")
                .spotId("spot-1")
                .socioUsername("lucas")
                .nombreSpot("FotoEstudio Bogotá")
                .titulo("50% de descuento")
                .imagenes(List.of("/img/1.jpg", "/img/2.jpg"))
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaFin(LocalDateTime.now().plusDays(30))
                .activo(true)
                .build();
    }

    @Test
    void toResponse_mapeaLaPrimeraImagenComoPrincipal() {
        PromocionResponseDTO dto = mapper.toResponse(promocionActiva());

        assertThat(dto.getId()).isEqualTo("p1");
        assertThat(dto.getSpotId()).isEqualTo("spot-1");
        assertThat(dto.getSocioUsername()).isEqualTo("lucas");
        assertThat(dto.getNombreSpot()).isEqualTo("FotoEstudio Bogotá");
        assertThat(dto.getTitulo()).isEqualTo("50% de descuento");
        assertThat(dto.getImagen()).isEqualTo("/img/1.jpg");
    }

    @Test
    void toResponse_sinImagenes_dejaImagenEnNull() {
        Promocion promocion = promocionActiva();
        promocion.setImagenes(List.of());

        PromocionResponseDTO dto = mapper.toResponse(promocion);

        assertThat(dto.getImagen()).isNull();
    }

    @Test
    void toResponse_inactiva_laClasificaComoDesactivada() {
        Promocion promocion = promocionActiva();
        promocion.setActivo(false);

        assertThat(mapper.toResponse(promocion).getEstado()).isEqualTo("DESACTIVADA");
    }

    @Test
    void toResponse_activaVigente_laClasificaComoActiva() {
        assertThat(mapper.toResponse(promocionActiva()).getEstado()).isEqualTo("ACTIVA");
    }

    @Test
    void toResponse_conFinPasado_laClasificaComoExpirada() {
        Promocion promocion = promocionActiva();
        promocion.setFechaFin(LocalDateTime.now().minusDays(1));

        assertThat(mapper.toResponse(promocion).getEstado()).isEqualTo("EXPIRADA");
    }

    @Test
    void toResponse_conInicioFuturo_laClasificaComoProxima() {
        Promocion promocion = promocionActiva();
        promocion.setFechaInicio(LocalDateTime.now().plusDays(5));
        promocion.setFechaFin(LocalDateTime.now().plusDays(10));

        assertThat(mapper.toResponse(promocion).getEstado()).isEqualTo("PROXIMA");
    }

    @Test
    void toResponseList_mapeaCadaElemento() {
        List<PromocionResponseDTO> resultado =
                mapper.toResponseList(List.of(promocionActiva(), promocionActiva()));

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allSatisfy(dto -> assertThat(dto.getId()).isEqualTo("p1"));
    }
}