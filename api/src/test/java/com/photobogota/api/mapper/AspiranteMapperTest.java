package com.photobogota.api.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.photobogota.api.dto.AspiranteResponseDTO;
import com.photobogota.api.model.Aspirante;
import com.photobogota.api.model.EstadoAspirante;

import static org.assertj.core.api.Assertions.assertThat;

class AspiranteMapperTest {

    private final AspiranteMapper mapper = new AspiranteMapper();

    private Aspirante aspiranteDeEjemplo() {
        return Aspirante.builder()
                .id("asp-1")
                .nombres("Juan")
                .apellidos("Romero")
                .email("juan.romero@example.com")
                .telefono("3001234567")
                .direccion("Calle 1")
                .nit("800123456-7")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .nombrePropietario("Juan Romero")
                .razonSocial("Café Romero S.A.S.")
                .categoria("Cafetería")
                .localidad("Kennedy")
                .rutaArchivo("/uploads/cedula.pdf")
                .tipoArchivo("pdf")
                .estado(EstadoAspirante.PENDIENTE)
                .fechaSolicitud(LocalDate.of(2026, 5, 3))
                .codigo("SOL-123456")
                .motivoDecision("Documento vencido")
                .decididoPor("mod1")
                .fechaDecision(LocalDateTime.of(2026, 5, 4, 10, 0))
                .fechaReenvio(LocalDateTime.of(2026, 5, 5, 11, 30))
                .vecesCorregida(2)
                .fechaEnvioCredenciales(LocalDateTime.of(2026, 5, 6, 9, 0))
                .nombreUsuarioGenerado("juan.romero")
                .comentariosInternos(List.of(
                        Aspirante.ComentarioInterno.builder()
                                .autor("mod1")
                                .texto("Parece legítimo")
                                .fecha(LocalDateTime.of(2026, 5, 3, 12, 0))
                                .build()))
                .build();
    }

    @Test
    void toResponse_mapeaLaEntidadCompleta() {
        AspiranteResponseDTO dto = mapper.toResponse(aspiranteDeEjemplo());

        assertThat(dto.getId()).isEqualTo("asp-1");
        assertThat(dto.getNombres()).isEqualTo("Juan");
        assertThat(dto.getApellidos()).isEqualTo("Romero");
        assertThat(dto.getEmail()).isEqualTo("juan.romero@example.com");
        assertThat(dto.getTelefono()).isEqualTo("3001234567");
        assertThat(dto.getDireccion()).isEqualTo("Calle 1");
        assertThat(dto.getNit()).isEqualTo("800123456-7");
        assertThat(dto.getFechaNacimiento()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(dto.getNombrePropietario()).isEqualTo("Juan Romero");
        assertThat(dto.getRazonSocial()).isEqualTo("Café Romero S.A.S.");
        assertThat(dto.getCategoria()).isEqualTo("Cafetería");
        assertThat(dto.getLocalidad()).isEqualTo("Kennedy");
        assertThat(dto.getRutaArchivo()).isEqualTo("/uploads/cedula.pdf");
        assertThat(dto.getTipoArchivo()).isEqualTo("pdf");
        assertThat(dto.getEstado()).isEqualTo(EstadoAspirante.PENDIENTE);
        assertThat(dto.getFechaSolicitud()).isEqualTo(LocalDate.of(2026, 5, 3));
        assertThat(dto.getCodigo()).isEqualTo("SOL-123456");
        assertThat(dto.getMotivoDecision()).isEqualTo("Documento vencido");
        assertThat(dto.getDecididoPor()).isEqualTo("mod1");
        assertThat(dto.getFechaDecision()).isEqualTo(LocalDateTime.of(2026, 5, 4, 10, 0));
        assertThat(dto.getFechaReenvio()).isEqualTo(LocalDateTime.of(2026, 5, 5, 11, 30));
        assertThat(dto.getVecesCorregida()).isEqualTo(2);
        assertThat(dto.getFechaEnvioCredenciales()).isEqualTo(LocalDateTime.of(2026, 5, 6, 9, 0));
        assertThat(dto.getNombreUsuarioGenerado()).isEqualTo("juan.romero");
        assertThat(dto.getComentariosInternos()).hasSize(1);
        assertThat(dto.getComentariosInternos().get(0).getAutor()).isEqualTo("mod1");
        assertThat(dto.getComentariosInternos().get(0).getTexto()).isEqualTo("Parece legítimo");
        assertThat(dto.getComentariosInternos().get(0).getFecha())
                .isEqualTo(LocalDateTime.of(2026, 5, 3, 12, 0));
    }

    @Test
    void toResponse_sinId_dejaElIdEnNull() {
        Aspirante aspirante = aspiranteDeEjemplo();
        aspirante.setId(null);

        AspiranteResponseDTO dto = mapper.toResponse(aspirante);

        assertThat(dto.getId()).isNull();
    }

    @Test
    void toResponse_sinComentarios_devuelveListaVacia() {
        Aspirante aspirante = aspiranteDeEjemplo();
        aspirante.setComentariosInternos(null);

        AspiranteResponseDTO dto = mapper.toResponse(aspirante);

        assertThat(dto.getComentariosInternos()).isEmpty();
    }

    @Test
    void toResponseList_mapeaCadaElemento() {
        List<Aspirante> aspirantes = List.of(aspiranteDeEjemplo(), aspiranteDeEjemplo());

        List<AspiranteResponseDTO> resultado = mapper.toResponseList(aspirantes);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allSatisfy(dto -> assertThat(dto.getId()).isEqualTo("asp-1"));
    }

    @Test
    void toResponseList_vacia_devuelveListaVacia() {
        assertThat(mapper.toResponseList(List.of())).isEmpty();
    }
}