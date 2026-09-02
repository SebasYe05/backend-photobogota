package com.photobogota.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.photobogota.api.dto.PromocionResponseDTO;
import com.photobogota.api.service.PromocionService;

import org.junit.jupiter.api.Test;

class PromocionControllerTest extends ControllerTestSupport {

    private final PromocionService promocionService = mock(PromocionService.class);
    private final PromocionController controller = new PromocionController(promocionService);

    private static final String PROMOCION_VALIDA =
            "{\"spotId\":\"loc1\",\"titulo\":\"50% de descuento\",\"descripcion\":\"Promo especial\","
                    + "\"fechaInicio\":\"2026-08-15\",\"fechaFin\":\"2026-09-15\",\"usosMaximos\":50}";

    @Test
    void listarActivas_devuelve200() throws Exception {
        when(promocionService.listarActivas()).thenReturn(java.util.List.of());

        mvc(controller)
                .perform(get("/api/v1/promociones"))
                .andExpect(status().isOk());
    }

    @Test
    void listarMias_devuelve200() throws Exception {
        when(promocionService.listarMias("socio1")).thenReturn(java.util.List.of());

        mvc(controller)
                .perform(get("/api/v1/promociones/mias").with(autenticado("socio1", "SOCIO")))
                .andExpect(status().isOk());
    }

    @Test
    void listarDeSpot_devuelve200() throws Exception {
        when(promocionService.listarDeSpot("loc1")).thenReturn(java.util.List.of());

        mvc(controller)
                .perform(get("/api/v1/promociones/spot/loc1"))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerActivaDeSpot_devuelve200() throws Exception {
        when(promocionService.obtenerActivaDeSpot("loc1")).thenReturn(mock(PromocionResponseDTO.class));

        mvc(controller)
                .perform(get("/api/v1/promociones/spot/loc1/activa"))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerPromocion_devuelve200() throws Exception {
        when(promocionService.obtenerPorId("p1")).thenReturn(mock(PromocionResponseDTO.class));

        mvc(controller)
                .perform(get("/api/v1/promociones/p1"))
                .andExpect(status().isOk());
    }

    @Test
    void crearPromocion_devuelve201() throws Exception {
        when(promocionService.crearPromocion(any(), eq("socio1")))
                .thenReturn(mock(PromocionResponseDTO.class));

        mvc(controller)
                .perform(json(post("/api/v1/promociones"), PROMOCION_VALIDA)
                        .with(autenticado("socio1", "SOCIO")))
                .andExpect(status().isCreated());
    }

    @Test
    void crearPromocion_conTituloVacio_devuelve400() throws Exception {
        mvc(controller)
                .perform(json(post("/api/v1/promociones"),
                        "{\"spotId\":\"loc1\",\"titulo\":\"\",\"descripcion\":\"x\","
                                + "\"fechaInicio\":\"2026-08-15\",\"fechaFin\":\"2026-09-15\"}")
                        .with(autenticado("socio1", "SOCIO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarPromocion_devuelve200() throws Exception {
        when(promocionService.actualizarPromocion(eq("p1"), any(), eq("socio1")))
                .thenReturn(mock(PromocionResponseDTO.class));

        mvc(controller)
                .perform(json(put("/api/v1/promociones/p1"), PROMOCION_VALIDA)
                        .with(autenticado("socio1", "SOCIO")))
                .andExpect(status().isOk());
    }

    @Test
    void togglePromocion_devuelve200() throws Exception {
        when(promocionService.desactivarPromocion("p1", "socio1"))
                .thenReturn(mock(PromocionResponseDTO.class));

        mvc(controller)
                .perform(patch("/api/v1/promociones/p1/toggle").with(autenticado("socio1", "SOCIO")))
                .andExpect(status().isOk());
    }

    @Test
    void duplicarPromocion_devuelve201() throws Exception {
        when(promocionService.duplicarPromocion("p1", "socio1"))
                .thenReturn(mock(PromocionResponseDTO.class));

        mvc(controller)
                .perform(post("/api/v1/promociones/p1/duplicar").with(autenticado("socio1", "SOCIO")))
                .andExpect(status().isCreated());
    }

    @Test
    void eliminarPromocion_devuelve204() throws Exception {
        mvc(controller)
                .perform(delete("/api/v1/promociones/p1").with(autenticado("socio1", "SOCIO")))
                .andExpect(status().isNoContent());
    }
}