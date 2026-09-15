package com.photobogota.api.mapper;

import java.time.LocalDate;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import com.photobogota.api.dto.PerfilUsuarioDTO;
import com.photobogota.api.dto.RegistroRequestDTO;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.Socio;
import com.photobogota.api.model.Usuario;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioMapperTest {

    private final UsuarioMapper mapper = new UsuarioMapperImpl();

    @Test
    void toDTO_miembro_mapeaConRolConstanteYConservaPuntosYNivel() {
        Miembro miembro = Miembro.builder()
                .id(new ObjectId("650000000000000000000001"))
                .nombresCompletos("Juan Romero")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .puntos(150L)
                .nivel(3)
                .build();

        PerfilUsuarioDTO dto = mapper.toDTO(miembro);

        assertThat(dto.getId()).isEqualTo(miembro.getId());
        assertThat(dto.getNombresCompletos()).isEqualTo("Juan Romero");
        assertThat(dto.getRol()).isEqualTo("MIEMBRO");
        assertThat(dto.getPuntos()).isEqualTo(150L);
        assertThat(dto.getNivel()).isEqualTo(3);
    }

    @Test
    void toDTO_usuarioMiembro_usaElSubclassMappingParaNoPerderCampos() {
        Miembro miembro = Miembro.builder()
                .id(new ObjectId("650000000000000000000002"))
                .puntos(42L)
                .nivel(2)
                .build();
        Usuario usuario = miembro;

        PerfilUsuarioDTO dto = mapper.toDTO(usuario);

        assertThat(dto.getRol()).isEqualTo("MIEMBRO");
        assertThat(dto.getPuntos()).isEqualTo(42L);
        assertThat(dto.getNivel()).isEqualTo(2);
    }

    @Test
    void toDTO_socio_derivaElRolDelNombreDeLaClase() {
        Socio socio = Socio.builder()
                .id(new ObjectId("650000000000000000000003"))
                .nombresCompletos("Lucas García")
                .build();
        Usuario usuario = socio;

        PerfilUsuarioDTO dto = mapper.toDTO(usuario);

        assertThat(dto.getRol()).isEqualTo("SOCIO");
        assertThat(dto.getNombresCompletos()).isEqualTo("Lucas García");
        assertThat(dto.getNivel()).isNull();
    }

    private RegistroRequestDTO registroDeEjemplo() {
        RegistroRequestDTO dto = new RegistroRequestDTO();
        dto.setNombresCompletos("Juan Romero");
        dto.setEmail("juan.romero@example.com");
        dto.setNombreUsuario("juan.romero");
        dto.setContrasena("Segura123.");
        dto.setFechaNacimiento(LocalDate.of(1990, 5, 15));
        return dto;
    }

    @Test
    void toMiembroEntity_aplicaLosDefaultsDeRegistro() {
        Miembro miembro = mapper.toMiembroEntity(registroDeEjemplo());

        assertThat(miembro.getId()).isNull();
        assertThat(miembro.getNombresCompletos()).isEqualTo("Juan Romero");
        assertThat(miembro.getFechaNacimiento()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(miembro.getEstadoCuenta()).isTrue();
        assertThat(miembro.getCorreoConfirmado()).isFalse();
        assertThat(miembro.getFechaRegistro()).isNotNull();
        assertThat(miembro.getTelefono()).isNull();
        assertThat(miembro.getBiografia()).isNull();
    }

    @Test
    void toEntity_delegaEnToMiembroEntity() {
        Usuario usuario = mapper.toEntity(registroDeEjemplo());

        assertThat(usuario).isInstanceOf(Miembro.class);
        assertThat(usuario.getNombresCompletos()).isEqualTo("Juan Romero");
        assertThat(usuario.getEstadoCuenta()).isTrue();
        assertThat(usuario.getCorreoConfirmado()).isFalse();
    }
}