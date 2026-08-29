package com.photobogota.api.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.photobogota.api.dto.LocalidadDTO;
import com.photobogota.api.model.Localidades;
import com.photobogota.api.repository.LocalidadesRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalidadServiceImplTest {

    @Mock
    private LocalidadesRepository localidadesRepository;

    @InjectMocks
    private LocalidadServiceImpl localidadService;

    private Localidades localidadDeEjemplo(String id, String nombre, boolean activo) {
        Localidades localidad = new Localidades();
        localidad.setId(id);
        localidad.setNombre(nombre);
        localidad.setDescripcion("Descripción de " + nombre);
        localidad.setImagen("/localidades/" + id + ".jpg");
        localidad.setActivo(activo);
        return localidad;
    }

    @Test
    void obtenerTodos_mapeaLasLocalidades() {
        when(localidadesRepository.findAll()).thenReturn(
                List.of(localidadDeEjemplo("loc-1", "Kennedy", true)));

        List<LocalidadDTO> resultado = localidadService.obtenerTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Kennedy");
        assertThat(resultado.get(0).getActivo()).isTrue();
    }

    @Test
    void obtenerActivos_soloDevuelveLasActivas() {
        when(localidadesRepository.findByActivoTrue()).thenReturn(
                List.of(localidadDeEjemplo("loc-1", "Chapinero", true)));

        List<LocalidadDTO> resultado = localidadService.obtenerActivos();

        assertThat(resultado).hasSize(1);
        verify(localidadesRepository).findByActivoTrue();
    }

    @Test
    void obtenerPorId_mapeaLaLocalidadEncontrada() {
        when(localidadesRepository.findById("loc-1"))
                .thenReturn(Optional.of(localidadDeEjemplo("loc-1", "Usaquén", true)));

        LocalidadDTO dto = localidadService.obtenerPorId("loc-1");

        assertThat(dto.getNombre()).isEqualTo("Usaquén");
    }

    @Test
    void obtenerPorId_inexistente_lanzaExcepcion() {
        when(localidadesRepository.findById("loc-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> localidadService.obtenerPorId("loc-x"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    void crear_nombreNuevo_guardaYRetornaDto() {
        when(localidadesRepository.existsByNombre("Kennedy")).thenReturn(false);
        when(localidadesRepository.save(any(Localidades.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalidadDTO dto = new LocalidadDTO();
        dto.setNombre("Kennedy");
        dto.setDescripcion("Localidad del occidente");

        LocalidadDTO resultado = localidadService.crear(dto);

        assertThat(resultado.getNombre()).isEqualTo("Kennedy");
        assertThat(resultado.getActivo()).isTrue();
    }

    @Test
    void crear_duplicado_lanzaExcepcionYNoGuarda() {
        when(localidadesRepository.existsByNombre("Kennedy")).thenReturn(true);

        LocalidadDTO dto = new LocalidadDTO();
        dto.setNombre("Kennedy");

        assertThatThrownBy(() -> localidadService.crear(dto))
                .isInstanceOf(RuntimeException.class);
        verify(localidadesRepository, never()).save(any());
    }

    @Test
    void actualizar_cambiosDeCampos_seAplican() {
        Localidades localidad = localidadDeEjemplo("loc-1", "Viejo", true);
        when(localidadesRepository.findById("loc-1")).thenReturn(Optional.of(localidad));
        when(localidadesRepository.save(any(Localidades.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalidadDTO dto = new LocalidadDTO();
        dto.setNombre("Nuevo");
        dto.setImagen("/localidades/nuevo.jpg");

        LocalidadDTO resultado = localidadService.actualizar("loc-1", dto);

        assertThat(resultado.getNombre()).isEqualTo("Nuevo");
        assertThat(resultado.getImagen()).isEqualTo("/localidades/nuevo.jpg");
    }

    @Test
    void eliminar_existente_eliminaDeLaBase() {
        when(localidadesRepository.existsById("loc-1")).thenReturn(true);

        localidadService.eliminar("loc-1");

        verify(localidadesRepository).deleteById("loc-1");
    }

    @Test
    void eliminar_inexistente_lanzaExcepcion() {
        when(localidadesRepository.existsById("loc-x")).thenReturn(false);

        assertThatThrownBy(() -> localidadService.eliminar("loc-x"))
                .isInstanceOf(RuntimeException.class);
        verify(localidadesRepository, never()).deleteById(any());
    }

    @Test
    void toggleStatus_invierteElValorActivo() {
        Localidades localidad = localidadDeEjemplo("loc-1", "Kennedy", true);
        when(localidadesRepository.findById("loc-1")).thenReturn(Optional.of(localidad));
        when(localidadesRepository.save(any(Localidades.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalidadDTO resultado = localidadService.toggleStatus("loc-1");

        assertThat(resultado.getActivo()).isFalse();
    }
}