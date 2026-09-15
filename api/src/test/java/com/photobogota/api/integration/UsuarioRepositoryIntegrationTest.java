package com.photobogota.api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.Usuario;
import com.photobogota.api.repository.UsuarioRepository;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataMongoTest
class UsuarioRepositoryIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @AfterEach
    void limpiar() {
        usuarioRepository.deleteAll();
    }

    private Miembro miembro(String nombresCompletos, boolean estadoCuenta) {
        return Miembro.builder()
                .nombresCompletos(nombresCompletos)
                .estadoCuenta(estadoCuenta)
                .puntos(100L)
                .nivel(1)
                .build();
    }

    @Test
    void guardarYBuscarPorId() {
        Miembro guardado = usuarioRepository.save(miembro("Juan Romero", true));

        Miembro encontrado = (Miembro) usuarioRepository.findById(guardado.getId()).orElse(null);

        assertThat(encontrado).isNotNull();
        assertThat(encontrado.getNombresCompletos()).isEqualTo("Juan Romero");
        assertThat(encontrado.getPuntos()).isEqualTo(100L);
        assertThat(encontrado.getEstadoCuenta()).isTrue();
        assertThat(encontrado.getContadorInfracciones()).isEqualTo(0);
    }

    @Test
    void buscarPorEstadoCuenta() {
        usuarioRepository.saveAll(java.util.List.of(
            miembro("Juan Romero", true),
            miembro("Ana Torres", true),
            miembro("Pedro Díaz", false)
        ));

        Page<Miembro> activos = (Page) usuarioRepository.findByEstadoCuenta(true, PageRequest.of(0, 10));
        Page<Miembro> inactivos = (Page) usuarioRepository.findByEstadoCuenta(false, PageRequest.of(0, 10));

        assertThat(activos.getTotalElements()).isEqualTo(2);
        assertThat(inactivos.getTotalElements()).isEqualTo(1);
    }

    @Test
    void buscarPorNombresCompletosIgnorandoMayusculas() {
        usuarioRepository.save(miembro("Juan Romero", true));

        Page<Miembro> resultado = (Page) usuarioRepository
                .findByNombresCompletosContainingIgnoreCase("jUAN", PageRequest.of(0, 10));

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getNombresCompletos()).isEqualTo("Juan Romero");
    }

    @Test
    void buscarQueNoExisteDevuelveVacio() {
        ObjectId inexistente = new ObjectId();

        java.util.Optional<Usuario> resultado = usuarioRepository.findById(inexistente);

        assertThat(resultado).isEmpty();
    }
}