package com.photobogota.api.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.photobogota.api.dto.EstadisticasSocioDTO;
import com.photobogota.api.service.EstadisticasSocioService;

import org.junit.jupiter.api.Test;

class EstadisticasControllerTest extends ControllerTestSupport {

    private final EstadisticasSocioService servicio = mock(EstadisticasSocioService.class);
    private final EstadisticasController controller = new EstadisticasController(servicio);

    @Test
    void obtenerEstadisticasSocio_devuelve200() throws Exception {
        when(servicio.obtenerEstadisticas("socio1", "mes")).thenReturn(new EstadisticasSocioDTO());

        mvc(controller)
                .perform(get("/api/v1/estadisticas/socio")
                        .param("periodo", "mes")
                        .with(autenticado("socio1", "SOCIO")))
                .andExpect(status().isOk());

        verify(servicio).obtenerEstadisticas("socio1", "mes");
    }

    @Test
    void obtenerEstadisticasSocio_sinPeriodo_usaElDefaultMes() throws Exception {
        when(servicio.obtenerEstadisticas("socio1", "mes")).thenReturn(new EstadisticasSocioDTO());

        mvc(controller)
                .perform(get("/api/v1/estadisticas/socio")
                        .with(autenticado("socio1", "SOCIO")))
                .andExpect(status().isOk());

        verify(servicio).obtenerEstadisticas("socio1", "mes");
    }
}