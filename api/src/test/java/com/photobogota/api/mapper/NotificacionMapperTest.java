package com.photobogota.api.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.photobogota.api.dto.NotificacionResponseDTO;
import com.photobogota.api.model.Notificacion;
import com.photobogota.api.model.NotificacionTipo;

import static org.assertj.core.api.Assertions.assertThat;

class NotificacionMapperTest {

    private final NotificacionMapper mapper = new NotificacionMapperImpl();

    private Notificacion notificacionDeEjemplo() {
        return Notificacion.builder()
                .id("n1")
                .destinatarioUsername("juan")
                .tipo(NotificacionTipo.NUEVA_RESENA)
                .titulo("Nueva reseña")
                .mensaje("Alguien dejó una reseña en tu spot")
                .spotId("spot-1")
                .emisorUsername("sistema")
                .leida(false)
                .fechaCreacion(LocalDateTime.of(2026, 8, 1, 12, 0))
                .build();
    }

    @Test
    void toResponse_mapeaElEnumTipoAString() {
        NotificacionResponseDTO dto = mapper.toResponse(notificacionDeEjemplo());

        assertThat(dto.getId()).isEqualTo("n1");
        assertThat(dto.getTipo()).isEqualTo("NUEVA_RESENA");
        assertThat(dto.getTitulo()).isEqualTo("Nueva reseña");
        assertThat(dto.getMensaje()).isEqualTo("Alguien dejó una reseña en tu spot");
        assertThat(dto.getSpotId()).isEqualTo("spot-1");
        assertThat(dto.getEmisorUsername()).isEqualTo("sistema");
        assertThat(dto.getLeida()).isFalse();
        assertThat(dto.getFechaCreacion()).isEqualTo(LocalDateTime.of(2026, 8, 1, 12, 0));
    }

    @Test
    void toResponse_mapeaElEstadoLeida() {
        Notificacion notificacion = notificacionDeEjemplo();
        notificacion.setLeida(true);

        NotificacionResponseDTO dto = mapper.toResponse(notificacion);

        assertThat(dto.getLeida()).isTrue();
    }

    @Test
    void toResponseList_mapeaCadaElemento() {
        List<NotificacionResponseDTO> resultado =
                mapper.toResponseList(List.of(notificacionDeEjemplo(), notificacionDeEjemplo()));

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allSatisfy(dto -> assertThat(dto.getId()).isEqualTo("n1"));
    }
}