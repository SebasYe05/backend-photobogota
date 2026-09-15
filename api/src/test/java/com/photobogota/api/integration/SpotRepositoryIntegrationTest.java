package com.photobogota.api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.photobogota.api.model.Spot;
import com.photobogota.api.repository.SpotRepository;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;

@DataMongoTest
class SpotRepositoryIntegrationTest {

    @Autowired
    private SpotRepository spotRepository;

    @AfterEach
    void limpiar() {
        spotRepository.deleteAll();
    }

    private Spot spot(String nombre, String categoria, String localidad) {
        Spot s = new Spot();
        s.setNombre(nombre);
        s.setCategoria(categoria);
        s.setLocalidad(localidad);
        s.setCreadorUsername("socio1");
        return s;
    }

    @Test
    void guardarYBuscarPorId() {
        Spot guardado = spotRepository.save(spot("Parque Simón Bolívar", "Parques", "Teusaquillo"));

        Spot encontrado = spotRepository.findById(guardado.getId()).orElse(null);

        assertThat(encontrado).isNotNull();
        assertThat(encontrado.getNombre()).isEqualTo("Parque Simón Bolívar");
        assertThat(encontrado.getImagenes()).isEmpty();
        assertThat(encontrado.getRating()).isEqualTo(0.0);
    }

    @Test
    void buscarPorCategoria() {
        spotRepository.saveAll(List.of(
            spot("Parque Simón Bolívar", "Parques", "Teusaquillo"),
            spot("Quinta de Bolívar", "Parques", "La Candelaria"),
            spot("Monserrate", "Montañas", "Santa Fe")
        ));

        List<Spot> parques = spotRepository.findByCategoria("Parques");

        assertThat(parques).hasSize(2);
    }

    @Test
    void buscarPorCategoriaYLocalidad() {
        spotRepository.saveAll(List.of(
            spot("A", "Museos", "La Candelaria"),
            spot("B", "Museos", "Chapinero"),
            spot("C", "Parques", "La Candelaria")
        ));

        List<Spot> museosCandelaria = spotRepository.findByCategoriaAndLocalidad("Museos", "La Candelaria");
        List<Spot> museosChapinero = spotRepository.findByCategoriaAndLocalidad("Museos", "Chapinero");

        assertThat(museosCandelaria).hasSize(1);
        assertThat(museosCandelaria.get(0).getNombre()).isEqualTo("A");
        assertThat(museosChapinero).hasSize(1);
        assertThat(museosChapinero.get(0).getNombre()).isEqualTo("B");
    }

    @Test
    void buscarPorNombreIgnorandoMayusculas() {
        spotRepository.save(spot("Monserrate", "Montañas", "Santa Fe"));

        List<Spot> resultado = spotRepository.findByNombreContainingIgnoreCase("mONs");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Monserrate");
    }

    @Test
    void contarSpotsPorCreador() {
        spotRepository.saveAll(List.of(
            spot("A", "Parques", "Teusaquillo"),
            spot("B", "Museos", "La Candelaria")
        ));
        spotRepository.save(spot("C", "Parques", "Chapinero"));

        long delSocio = spotRepository.countByCreadorUsername("socio1");
        long deOtro = spotRepository.countByCreadorUsername("otro");

        assertThat(delSocio).isEqualTo(3);
        assertThat(deOtro).isZero();
    }

    @Test
    void buscarPorCreador() {
        Spot s1 = spotRepository.save(spot("A", "Parques", "Teusaquillo"));
        s1.setCreadorUsername("socio1");
        spotRepository.save(s1);
        spotRepository.save(spot("B", "Museos", "La Candelaria"));

        List<Spot> delSocio = spotRepository.findByCreadorUsername("socio1");

        assertThat(delSocio).hasSize(2);
    }

    @Test
    void guardarResenasEnEmbeddedDocumento() {
        Spot s = spotRepository.save(spot("Monserrate", "Montañas", "Santa Fe"));
        Spot.Resena resena = new Spot.Resena();
        resena.setUsuario("juan");
        resena.setRating(5);
        resena.setComentario("Vista espectacular");
        s.getResenas().add(resena);
        s.setTotalResenas(1);
        s.setRating(5.0);
        spotRepository.save(s);

        Spot recargado = spotRepository.findById(s.getId()).orElseThrow();

        assertThat(recargado.getResenas()).hasSize(1);
        assertThat(recargado.getResenas().get(0).getUsuario()).isEqualTo("juan");
        assertThat(recargado.getRating()).isEqualTo(5.0);
        assertThat(recargado.getTotalResenas()).isEqualTo(1);
    }
}