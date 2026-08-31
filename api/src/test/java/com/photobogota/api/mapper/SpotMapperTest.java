package com.photobogota.api.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.photobogota.api.dto.SpotResumenDTO;
import com.photobogota.api.dto.SpotResponseDTO;
import com.photobogota.api.model.Spot;

import static org.assertj.core.api.Assertions.assertThat;

class SpotMapperTest {

    private final SpotMapper mapper = new SpotMapperImpl();

    private Spot spotDeEjemplo() {
        Spot spot = new Spot();
        spot.setId("spot-1");
        spot.setNombre("Parque Simón Bolívar");
        spot.setLatitud(3.8667);
        spot.setLongitud(-73.0500);
        spot.setDireccion("Calle 63 #68-95");
        spot.setCategoria("Parque");
        spot.setLocalidad("Barrios Unidos");
        spot.setDescripcion("Parque al aire libre");
        spot.setTipo("SPOT");
        spot.setImagenes(List.of("/img/spot1.jpg", "/img/spot2.jpg"));
        spot.setCreadorUsername("rocky");
        spot.setCreadorRol("MIEMBRO");
        spot.setCreadoEn(LocalDateTime.of(2026, 8, 1, 10, 0));
        return spot;
    }

    @Test
    void toResumen_mapeaLosCamposBaseYCreador() {
        Spot spot = spotDeEjemplo();
        spot.setCreadoEn(LocalDateTime.now().minusDays(3));

        SpotResumenDTO dto = mapper.toResumen(spot);

        assertThat(dto.getId()).isEqualTo("spot-1");
        assertThat(dto.getNombre()).isEqualTo("Parque Simón Bolívar");
        assertThat(dto.getLatitud()).isEqualTo(3.8667);
        assertThat(dto.getLongitud()).isEqualTo(-73.0500);
        assertThat(dto.getImagen()).isEqualTo("/img/spot1.jpg");
        assertThat(dto.getUsuarioId()).isEqualTo("rocky");
        assertThat(dto.getRol()).isEqualTo("MIEMBRO");
        assertThat(dto.getTipo()).isEqualTo("SPOT");
        assertThat(dto.getTienePromocion()).isFalse();
        assertThat(dto.getCreatedAt()).isEqualTo("Hace 3 días");
        assertThat(dto.getCreador().getNombreUsuario()).isEqualTo("rocky");
        assertThat(dto.getCreador().getRol()).isEqualTo("MIEMBRO");
    }

    @Test
    void toResumen_sinTipo_creadoPorSocio_normalizaALocal() {
        Spot spot = spotDeEjemplo();
        spot.setTipo(null);
        spot.setCreadorRol("SOCIO");

        assertThat(mapper.toResumen(spot).getTipo()).isEqualTo("LOCAL");
    }

    @Test
    void toResumen_sinTipo_creadoPorMiembro_normalizaASpot() {
        Spot spot = spotDeEjemplo();
        spot.setTipo(null);

        assertThat(mapper.toResumen(spot).getTipo()).isEqualTo("SPOT");
    }

    @Test
    void toResumen_tipoEnBlanco_normalizaSegunRol() {
        Spot spot = spotDeEjemplo();
        spot.setTipo("   ");

        assertThat(mapper.toResumen(spot).getTipo()).isEqualTo("SPOT");
    }

    @Test
    void toResumen_tipoPresente_loRespeta() {
        Spot spot = spotDeEjemplo();
        spot.setTipo("EXOTICO");

        assertThat(mapper.toResumen(spot).getTipo()).isEqualTo("EXOTICO");
    }

    @Test
    void toResumen_sinImagenes_dejaImagenEnNull() {
        Spot spot = spotDeEjemplo();
        spot.setImagenes(List.of());

        assertThat(mapper.toResumen(spot).getImagen()).isNull();
    }

    @Test
    void toResumen_sinCreadorUsername_creadorNull() {
        Spot spot = spotDeEjemplo();
        spot.setCreadorUsername(null);

        SpotResumenDTO dto = mapper.toResumen(spot);

        assertThat(dto.getCreador()).isNull();
        assertThat(dto.getUsuarioId()).isNull();
    }

    @Test
    void toResumenList_mapeaCadaElemento() {
        List<SpotResumenDTO> resultado = mapper.toResumenList(List.of(spotDeEjemplo(), spotDeEjemplo()));

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allSatisfy(dto -> assertThat(dto.getId()).isEqualTo("spot-1"));
    }

    @Test
    void toResponse_mapeaLosCamposBase() {
        Spot spot = spotDeEjemplo();
        spot.setCreadoEn(LocalDateTime.now().minusDays(3));

        SpotResponseDTO dto = mapper.toResponse(spot);

        assertThat(dto.getId()).isEqualTo("spot-1");
        assertThat(dto.getNombre()).isEqualTo("Parque Simón Bolívar");
        assertThat(dto.getImagen()).isEqualTo("/img/spot1.jpg");
        assertThat(dto.getTipo()).isEqualTo("SPOT");
        assertThat(dto.getCreador().getNombreUsuario()).isEqualTo("rocky");
        assertThat(dto.getCreatedAt()).isEqualTo("Hace 3 días");
    }

    private Spot.Resena resenaConFecha(LocalDateTime fecha) {
        Spot.Resena resena = new Spot.Resena();
        resena.setId("r1");
        resena.setUsuario("maria");
        resena.setAvatar("/avatars/maria.jpg");
        resena.setRating(5);
        resena.setComentario("¡Excelente!");
        resena.setFecha(fecha);
        return resena;
    }

    @Test
    void toResenaResponse_sinFecha_laFormateaComoRecientemente() {
        SpotResponseDTO.ResenaResponseDTO dto = mapper.toResenaResponse(resenaConFecha(null));

        assertThat(dto.getFecha()).isEqualTo("Recientemente");
        assertThat(dto.getUsuario()).isEqualTo("maria");
        assertThat(dto.getRating()).isEqualTo(5);
    }

    @Test
    void toResenaResponse_fechaDeHoy_seFormateaComoHoy() {
        assertThat(mapper.toResenaResponse(resenaConFecha(LocalDateTime.now())).getFecha())
                .isEqualTo("Hoy");
    }

    @Test
    void toResenaResponse_fechaDeAyer_seFormateaComoAyer() {
        assertThat(mapper.toResenaResponse(resenaConFecha(LocalDateTime.now().minusDays(1))).getFecha())
                .isEqualTo("Ayer");
    }

    @Test
    void toResenaResponse_fechaReciente_seFormateaEnDias() {
        assertThat(mapper.toResenaResponse(resenaConFecha(LocalDateTime.now().minusDays(3))).getFecha())
                .isEqualTo("Hace 3 días");
    }

    @Test
    void toResenaResponse_fechaVieja_seFormateaEnSemanas() {
        assertThat(mapper.toResenaResponse(resenaConFecha(LocalDateTime.now().minusDays(15))).getFecha())
                .isEqualTo("Hace 2 semanas");
    }

    @Test
    void toResenaResponse_fechaAntigua_seFormateaEnMeses() {
        assertThat(mapper.toResenaResponse(resenaConFecha(LocalDateTime.now().minusDays(61))).getFecha())
                .isEqualTo("Hace 2 meses");
    }

    @Test
    void toResponse_mapeaLasResenas() {
        Spot spot = spotDeEjemplo();
        spot.setResenas(List.of(resenaConFecha(LocalDateTime.now())));

        SpotResponseDTO dto = mapper.toResponse(spot);

        assertThat(dto.getResenas()).hasSize(1);
        assertThat(dto.getResenas().get(0).getComentario()).isEqualTo("¡Excelente!");
        assertThat(dto.getResenas().get(0).getFecha()).isEqualTo("Hoy");
    }
}