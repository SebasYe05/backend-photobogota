package com.photobogota.api.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.photobogota.api.dto.CategoriaDTO;
import com.photobogota.api.dto.LocalidadDTO;
import com.photobogota.api.service.ICategoriaService;
import com.photobogota.api.service.ILocalidadService;

import org.junit.jupiter.api.Test;

class CategoriaControllerTest extends ControllerTestSupport {

    private final ICategoriaService categoriaService = mock(ICategoriaService.class);
    private final ILocalidadService localidadService = mock(ILocalidadService.class);
    private final CategoriaController controller = new CategoriaController(categoriaService, localidadService);

    @Test
    void listarCategoriasActivas_devuelve200() throws Exception {
        when(categoriaService.obtenerActivos()).thenReturn(List.of(mock(CategoriaDTO.class)));

        mvc(controller)
                .perform(get("/api/v1/categorias"))
                .andExpect(status().isOk());
    }

    @Test
    void listarLocalidadesActivas_devuelve200() throws Exception {
        when(localidadService.obtenerActivos()).thenReturn(List.of(mock(LocalidadDTO.class)));

        mvc(controller)
                .perform(get("/api/v1/localidades"))
                .andExpect(status().isOk());
    }
}