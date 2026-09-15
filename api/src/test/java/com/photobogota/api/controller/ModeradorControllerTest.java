package com.photobogota.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.photobogota.api.dto.CategoriaDTO;
import com.photobogota.api.dto.LocalidadDTO;
import com.photobogota.api.service.ICategoriaService;
import com.photobogota.api.service.ILocalidadService;

import org.junit.jupiter.api.Test;

class ModeradorControllerTest extends ControllerTestSupport {

    private final ICategoriaService categoriaService = mock(ICategoriaService.class);
    private final ILocalidadService localidadService = mock(ILocalidadService.class);
    private final ModeradorController controller = new ModeradorController(categoriaService, localidadService);

    @Test
    void listarCategorias_devuelve200() throws Exception {
        when(categoriaService.obtenerTodos()).thenReturn(List.of(mock(CategoriaDTO.class)));

        mvc(controller)
                .perform(get("/api/v1/moderador/categorias").with(autenticado("mod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void listarCategoriasActivas_devuelve200() throws Exception {
        when(categoriaService.obtenerActivos()).thenReturn(List.of());

        mvc(controller)
                .perform(get("/api/v1/moderador/categorias/activas").with(autenticado("mod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerCategoria_devuelve200() throws Exception {
        when(categoriaService.obtenerPorId("c1")).thenReturn(mock(CategoriaDTO.class));

        mvc(controller)
                .perform(get("/api/v1/moderador/categorias/c1").with(autenticado("mod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void crearCategoria_devuelve201() throws Exception {
        when(categoriaService.crear(any())).thenReturn(mock(CategoriaDTO.class));

        mvc(controller)
                .perform(json(post("/api/v1/moderador/categorias"),
                        "{\"nombre\":\"Paisaje urbano\"}").with(autenticado("mod", "MOD")))
                .andExpect(status().isCreated());

        verify(categoriaService).crear(any());
    }

    @Test
    void actualizarCategoria_devuelve200() throws Exception {
        when(categoriaService.actualizar(eq("c1"), any())).thenReturn(mock(CategoriaDTO.class));

        mvc(controller)
                .perform(json(put("/api/v1/moderador/categorias/c1"),
                        "{\"nombre\":\"Naturaleza\"}").with(autenticado("mod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void eliminarCategoria_devuelve204() throws Exception {
        mvc(controller)
                .perform(delete("/api/v1/moderador/categorias/c1").with(autenticado("mod", "MOD")))
                .andExpect(status().isNoContent());

        verify(categoriaService).eliminar("c1");
    }

    @Test
    void toggleCategoria_devuelve200() throws Exception {
        when(categoriaService.togglestatus("c1")).thenReturn(mock(CategoriaDTO.class));

        mvc(controller)
                .perform(patch("/api/v1/moderador/categorias/c1/toggle").with(autenticado("mod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void listarLocalidades_devuelve200() throws Exception {
        when(localidadService.obtenerTodos()).thenReturn(List.of(mock(LocalidadDTO.class)));

        mvc(controller)
                .perform(get("/api/v1/moderador/localidades").with(autenticado("mod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void listarLocalidadesActivas_devuelve200() throws Exception {
        when(localidadService.obtenerActivos()).thenReturn(List.of());

        mvc(controller)
                .perform(get("/api/v1/moderador/localidades/activas").with(autenticado("mod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerLocalidad_devuelve200() throws Exception {
        when(localidadService.obtenerPorId("l1")).thenReturn(mock(LocalidadDTO.class));

        mvc(controller)
                .perform(get("/api/v1/moderador/localidades/l1").with(autenticado("mod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void crearLocalidad_devuelve201() throws Exception {
        when(localidadService.crear(any())).thenReturn(mock(LocalidadDTO.class));

        mvc(controller)
                .perform(json(post("/api/v1/moderador/localidades"),
                        "{\"nombre\":\"Kennedy\"}").with(autenticado("mod", "MOD")))
                .andExpect(status().isCreated());
    }

    @Test
    void actualizarLocalidad_devuelve200() throws Exception {
        when(localidadService.actualizar(eq("l1"), any())).thenReturn(mock(LocalidadDTO.class));

        mvc(controller)
                .perform(json(put("/api/v1/moderador/localidades/l1"),
                        "{\"nombre\":\"Chapinero\"}").with(autenticado("mod", "MOD")))
                .andExpect(status().isOk());
    }

    @Test
    void eliminarLocalidad_devuelve204() throws Exception {
        mvc(controller)
                .perform(delete("/api/v1/moderador/localidades/l1").with(autenticado("mod", "MOD")))
                .andExpect(status().isNoContent());

        verify(localidadService).eliminar("l1");
    }

    @Test
    void toggleLocalidad_devuelve200() throws Exception {
        when(localidadService.toggleStatus("l1")).thenReturn(mock(LocalidadDTO.class));

        mvc(controller)
                .perform(patch("/api/v1/moderador/localidades/l1/toggle").with(autenticado("mod", "MOD")))
                .andExpect(status().isOk());
    }
}