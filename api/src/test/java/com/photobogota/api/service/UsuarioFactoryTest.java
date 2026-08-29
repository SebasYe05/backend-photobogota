package com.photobogota.api.service;

import java.time.LocalDate;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import com.photobogota.api.dto.CrearUsuarioRequestDTO;
import com.photobogota.api.model.Admin;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.Moderador;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.Socio;
import com.photobogota.api.model.Usuario;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioFactoryTest {

    private final UsuarioFactory usuarioFactory = new UsuarioFactory();

    private CrearUsuarioRequestDTO dtoDeEjemplo() {
        CrearUsuarioRequestDTO dto = new CrearUsuarioRequestDTO();
        dto.setNombresCompletos("Juan Pérez");
        dto.setFechaNacimiento(LocalDate.of(1995, 5, 20));
        return dto;
    }

    private void verificarCamposBase(Usuario usuario, ObjectId id) {
        assertThat(usuario.getId()).isEqualTo(id);
        assertThat(usuario.getNombresCompletos()).isEqualTo("Juan Pérez");
        assertThat(usuario.getFechaNacimiento()).isEqualTo(LocalDate.of(1995, 5, 20));
        assertThat(usuario.getEstadoCuenta()).isTrue();
        assertThat(usuario.getCorreoConfirmado()).isFalse();
    }

    @Test
    void crearUsuario_conRolAdmin_creaUnAdmin() {
        ObjectId id = new ObjectId();

        Usuario usuario = usuarioFactory.crearUsuario(id, dtoDeEjemplo(), Rol.ADMIN);

        assertThat(usuario).isInstanceOf(Admin.class);
        verificarCamposBase(usuario, id);
        assertThat(((Admin) usuario).getUltimaConexionPanel()).isNull();
    }

    @Test
    void crearUsuario_conRolSocio_creaUnSocio() {
        ObjectId id = new ObjectId();

        Usuario usuario = usuarioFactory.crearUsuario(id, dtoDeEjemplo(), Rol.SOCIO);

        assertThat(usuario).isInstanceOf(Socio.class);
        verificarCamposBase(usuario, id);
    }

    @Test
    void crearUsuario_conRolModerador_creaUnModerador() {
        ObjectId id = new ObjectId();

        Usuario usuario = usuarioFactory.crearUsuario(id, dtoDeEjemplo(), Rol.MOD);

        assertThat(usuario).isInstanceOf(Moderador.class);
        verificarCamposBase(usuario, id);
    }

    @Test
    void crearUsuario_conRolMiembro_creaUnMiembroConNivelYPuntosIniciales() {
        ObjectId id = new ObjectId();

        Usuario usuario = usuarioFactory.crearUsuario(id, dtoDeEjemplo(), Rol.MIEMBRO);

        assertThat(usuario).isInstanceOf(Miembro.class);
        verificarCamposBase(usuario, id);
        Miembro miembro = (Miembro) usuario;
        assertThat(miembro.getPuntos()).isEqualTo(0L);
        assertThat(miembro.getNivel()).isEqualTo(1);
    }
}