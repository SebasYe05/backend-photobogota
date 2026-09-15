package com.photobogota.api.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.photobogota.api.dto.CategoriaDTO;
import com.photobogota.api.model.Categoria;
import com.photobogota.api.repository.CategoriaRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceImplTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaServiceImpl categoriaService;

    private Categoria categoriaDeEjemplo(String id, String nombre, boolean activo) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNombre(nombre);
        categoria.setDescripcion("Descripción de " + nombre);
        categoria.setImagen("/categorias/" + id + ".jpg");
        categoria.setActivo(activo);
        return categoria;
    }

    @Test
    void obtenerTodos_construyeDtosConLosCamposMapeados() {
        when(categoriaRepository.findAll()).thenReturn(
                List.of(categoriaDeEjemplo("cat-1", "Paisaje urbano", true)));

        List<CategoriaDTO> resultado = categoriaService.obtenerTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Paisaje urbano");
        assertThat(resultado.get(0).getActivo()).isTrue();
    }

    @Test
    void obtenerActivos_soloDevuelveLasActivas() {
        when(categoriaRepository.findByActivoTrue()).thenReturn(
                List.of(categoriaDeEjemplo("cat-1", "Paisaje urbano", true)));

        List<CategoriaDTO> resultado = categoriaService.obtenerActivos();

        assertThat(resultado).hasSize(1);
        verify(categoriaRepository).findByActivoTrue();
    }

    @Test
    void obtenerPorId_mapeaLaCategoriaEncontrada() {
        when(categoriaRepository.findById("cat-1")).thenReturn(Optional.of(categoriaDeEjemplo("cat-1", "Caldos", true)));

        CategoriaDTO dto = categoriaService.obtenerPorId("cat-1");

        assertThat(dto.getNombre()).isEqualTo("Caldos");
    }

    @Test
    void obtenerPorId_categoriaInexistente_lanzaExcepcion() {
        when(categoriaRepository.findById("cat-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.obtenerPorId("cat-x"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Categoría no encontrada");
    }

    @Test
    void crear_nombreNuevo_guardaYRetornaDto() {
        when(categoriaRepository.existsByNombre("Parque")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoriaDTO dto = new CategoriaDTO();
        dto.setNombre("Parque");
        dto.setDescripcion("Zonas verdes");
        dto.setImagen("/categorias/parque.jpg");

        CategoriaDTO resultado = categoriaService.crear(dto);

        assertThat(resultado.getNombre()).isEqualTo("Parque");
        assertThat(resultado.getActivo()).isTrue();
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    void crear_duplicado_lanzaExcepcionYNoGuarda() {
        when(categoriaRepository.existsByNombre("Parque")).thenReturn(true);

        CategoriaDTO dto = new CategoriaDTO();
        dto.setNombre("Parque");

        assertThatThrownBy(() -> categoriaService.crear(dto))
                .isInstanceOf(RuntimeException.class);
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void actualizar_cambiosDeCampos_seAplican() {
        Categoria categoria = categoriaDeEjemplo("cat-1", "Viejo", true);
        when(categoriaRepository.findById("cat-1")).thenReturn(Optional.of(categoria));
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoriaDTO dto = new CategoriaDTO();
        dto.setNombre("Nuevo");
        dto.setDescripcion("Nueva descripción");

        CategoriaDTO resultado = categoriaService.actualizar("cat-1", dto);

        assertThat(resultado.getNombre()).isEqualTo("Nuevo");
        assertThat(resultado.getDescripcion()).isEqualTo("Nueva descripción");
    }

    @Test
    void eliminar_existente_eliminaDeLaBase() {
        when(categoriaRepository.existsById("cat-1")).thenReturn(true);

        categoriaService.eliminar("cat-1");

        verify(categoriaRepository).deleteById("cat-1");
    }

    @Test
    void eliminar_inexistente_lanzaExcepcion() {
        when(categoriaRepository.existsById("cat-x")).thenReturn(false);

        assertThatThrownBy(() -> categoriaService.eliminar("cat-x"))
                .isInstanceOf(RuntimeException.class);
        verify(categoriaRepository, never()).deleteById(any());
    }

    @Test
    void togglestatus_invierteElValorActivo() {
        Categoria categoria = categoriaDeEjemplo("cat-1", "Parque", true);
        when(categoriaRepository.findById("cat-1")).thenReturn(Optional.of(categoria));
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoriaDTO resultado = categoriaService.togglestatus("cat-1");

        assertThat(resultado.getActivo()).isFalse();
    }
}