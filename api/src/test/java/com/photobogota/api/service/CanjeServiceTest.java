package com.photobogota.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.photobogota.api.dto.CanjeResponseDTO;
import com.photobogota.api.dto.CanjearRequestDTO;
import com.photobogota.api.dto.ValidarCanjeRequestDTO;
import com.photobogota.api.exception.AccessForbiddenException;
import com.photobogota.api.exception.OperacionInvalidaException;
import com.photobogota.api.exception.RecursoCaducadoException;
import com.photobogota.api.exception.RecursoNoDisponibleException;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.mapper.CanjeMapperImpl;
import com.photobogota.api.model.Canje;
import com.photobogota.api.model.Promocion;
import com.photobogota.api.model.Spot;
import com.photobogota.api.repository.CanjeRepository;
import com.photobogota.api.repository.PromocionRepository;
import com.photobogota.api.repository.SpotRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CanjeServiceTest {

    @Mock
    private CanjeRepository canjeRepository;

    @Mock
    private PromocionRepository promocionRepository;

    @Mock
    private SpotRepository spotRepository;

    private CanjeService canjeService;

    private Promocion promocionActiva;

    @BeforeEach
    void setUp() {
        canjeService = new CanjeService(canjeRepository, promocionRepository, spotRepository,
                new CanjeMapperImpl());

        promocionActiva = Promocion.builder()
                .id("p1")
                .spotId("s1")
                .socioUsername("socio1")
                .nombreSpot("FotoEstudio Bogotá")
                .titulo("50% de descuento en sesión de fotos")
                .descuento("50%")
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaFin(LocalDateTime.now().plusDays(30))
                .activo(true)
                .usos(0)
                .usosMaximos(50)
                .build();
    }

    private Spot spotPropio() {
        Spot spot = new Spot();
        spot.setId("s1");
        spot.setNombre("FotoEstudio Bogotá");
        spot.setCreadorUsername("socio1");
        spot.setCreadorRol("SOCIO");
        return spot;
    }

    // --- Flujo principal: miembro canjea → socio valida ---

    @Test
    void canjearYValidar_flujoCompletoMiembroSocio_funciona() {
        CanjearRequestDTO canjear = new CanjearRequestDTO();
        canjear.setPromocionId("p1");

        when(promocionRepository.findById("p1")).thenReturn(Optional.of(promocionActiva));
        when(canjeRepository.findByPromocionIdAndMiembroNombre("p1", "miembro1")).thenReturn(List.of());
        when(promocionRepository.save(any(Promocion.class))).thenAnswer(inv -> inv.getArgument(0));

        Canje nuevo = new Canje();
        when(canjeRepository.save(any(Canje.class))).thenAnswer(inv -> {
            Canje canje = inv.getArgument(0);
            canje.setId("c1");
            return canje;
        });

        CanjeResponseDTO canjeado = canjeService.crearCanje(canjear, "miembro1");

        assertThat(canjeado).isNotNull();
        assertThat(canjeado.getCodigo()).hasSize(8).matches("[A-Z0-9]{8}");
        assertThat(canjeado.getEstado()).isEqualTo("VIGENTE");
        assertThat(canjeado.getFechaExpiracion()).isEqualTo(promocionActiva.getFechaFin());
        assertThat(canjeado.getPromocionId()).isEqualTo("p1");
        assertThat(canjeado.getPromocionTitulo()).isEqualTo("50% de descuento en sesión de fotos");
        assertThat(canjeado.getDescuento()).isEqualTo("50%");
        assertThat(canjeado.getSpotId()).isEqualTo("s1");
        assertThat(canjeado.getSpotNombre()).isEqualTo("FotoEstudio Bogotá");
        assertThat(canjeado.getMiembroNombre()).isEqualTo("miembro1");
        assertThat(promocionActiva.getUsos()).isEqualTo(1);

        String codigo = canjeado.getCodigo();

        // El socio dueño valida el código presentado por el miembro.
        Spot spot = spotPropio();
        when(spotRepository.findById("s1")).thenReturn(Optional.of(spot));
        when(promocionRepository.findById("p1")).thenReturn(Optional.of(promocionActiva));

        ValidarCanjeRequestDTO validar = new ValidarCanjeRequestDTO();
        validar.setCodigo(codigo);
        validar.setSpotId("s1");
        Canje canje = Canje.builder()
                .id("c1").codigo(codigo).estado("VIGENTE")
                .fechaCanje(LocalDateTime.now())
                .fechaExpiracion(promocionActiva.getFechaFin())
                .promocionId("p1").promocionTitulo("50% de descuento en sesión de fotos")
                .descuento("50%").spotId("s1").spotNombre("FotoEstudio Bogotá")
                .miembroNombre("miembro1").socioUsername("socio1")
                .build();
        when(canjeRepository.findByCodigoIgnoreCase(codigo)).thenReturn(Optional.of(canje));
        when(canjeRepository.save(any(Canje.class))).thenAnswer(inv -> inv.getArgument(0));

        CanjeResponseDTO validado = canjeService.validarCanje(validar, "socio1");

        assertThat(validado.getEstado()).isEqualTo("USADO");
        assertThat(validado.getCodigo()).isEqualTo(codigo);
    }

    // --- Creación de canjes ---

    @Test
    void crearCanje_promocionDesactivada_lanza400() {
        promocionActiva.setActivo(false);
        when(promocionRepository.findById("p1")).thenReturn(Optional.of(promocionActiva));

        CanjearRequestDTO request = new CanjearRequestDTO();
        request.setPromocionId("p1");

        assertThatThrownBy(() -> canjeService.crearCanje(request, "miembro1"))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void crearCanje_promocionFueraDeFechas_lanza400() {
        promocionActiva.setFechaFin(LocalDateTime.now().minusDays(1));
        when(promocionRepository.findById("p1")).thenReturn(Optional.of(promocionActiva));

        CanjearRequestDTO request = new CanjearRequestDTO();
        request.setPromocionId("p1");

        assertThatThrownBy(() -> canjeService.crearCanje(request, "miembro1"))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void crearCanje_promocionAgotada_lanza409() {
        promocionActiva.setUsos(50);
        when(promocionRepository.findById("p1")).thenReturn(Optional.of(promocionActiva));

        CanjearRequestDTO request = new CanjearRequestDTO();
        request.setPromocionId("p1");

        assertThatThrownBy(() -> canjeService.crearCanje(request, "miembro1"))
                .isInstanceOf(RecursoNoDisponibleException.class);
    }

    @Test
    void crearCanje_yaTieneCanjeVigente_lanza409() {
        when(promocionRepository.findById("p1")).thenReturn(Optional.of(promocionActiva));
        when(canjeRepository.findByPromocionIdAndMiembroNombre("p1", "miembro1")).thenReturn(List.of(
                Canje.builder()
                        .estado("VIGENTE")
                        .fechaExpiracion(LocalDateTime.now().plusDays(5))
                        .build()));

        CanjearRequestDTO request = new CanjearRequestDTO();
        request.setPromocionId("p1");

        assertThatThrownBy(() -> canjeService.crearCanje(request, "miembro1"))
                .isInstanceOf(RecursoNoDisponibleException.class);
    }

    @Test
    void crearCanje_canjeYaUsado_lanza409() {
        when(promocionRepository.findById("p1")).thenReturn(Optional.of(promocionActiva));
        when(canjeRepository.findByPromocionIdAndMiembroNombre("p1", "miembro1")).thenReturn(List.of(
                Canje.builder().estado("USADO").build()));

        CanjearRequestDTO request = new CanjearRequestDTO();
        request.setPromocionId("p1");

        assertThatThrownBy(() -> canjeService.crearCanje(request, "miembro1"))
                .isInstanceOf(RecursoNoDisponibleException.class);
    }

    @Test
    void crearCanje_canjeExpiradoPrevio_noBloqueaNuevoCanje() {
        when(promocionRepository.findById("p1")).thenReturn(Optional.of(promocionActiva));
        when(canjeRepository.findByPromocionIdAndMiembroNombre("p1", "miembro1")).thenReturn(List.of(
                Canje.builder().estado("VIGENTE").fechaExpiracion(LocalDateTime.now().minusDays(1)).build()));
        when(promocionRepository.save(any(Promocion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(canjeRepository.save(any(Canje.class))).thenAnswer(inv -> inv.getArgument(0));

        CanjearRequestDTO request = new CanjearRequestDTO();
        request.setPromocionId("p1");

        CanjeResponseDTO respuesta = canjeService.crearCanje(request, "miembro1");

        assertThat(respuesta.getEstado()).isEqualTo("VIGENTE");
    }

    // --- Validación de canjes ---

    @Test
    void validarCanje_localAjeno_lanza403() {
        Spot spot = new Spot();
        spot.setId("s1");
        spot.setCreadorUsername("socio2");
        when(spotRepository.findById("s1")).thenReturn(Optional.of(spot));

        ValidarCanjeRequestDTO request = new ValidarCanjeRequestDTO();
        request.setCodigo("ABC12345");
        request.setSpotId("s1");

        assertThatThrownBy(() -> canjeService.validarCanje(request, "socio1"))
                .isInstanceOf(AccessForbiddenException.class);
    }

    @Test
    void validarCanje_codigoNoExiste_lanza404() {
        when(spotRepository.findById("s1")).thenReturn(Optional.of(spotPropio()));
        when(canjeRepository.findByCodigoIgnoreCase("ABC12345")).thenReturn(Optional.empty());

        ValidarCanjeRequestDTO request = new ValidarCanjeRequestDTO();
        request.setCodigo("ABC12345");
        request.setSpotId("s1");

        assertThatThrownBy(() -> canjeService.validarCanje(request, "socio1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void validarCanje_codigoDeOtroLocal_lanza400() {
        when(spotRepository.findById("s1")).thenReturn(Optional.of(spotPropio()));
        when(canjeRepository.findByCodigoIgnoreCase("ABC12345")).thenReturn(Optional.of(
                Canje.builder().estado("VIGENTE").spotId("s-otro").build()));

        ValidarCanjeRequestDTO request = new ValidarCanjeRequestDTO();
        request.setCodigo("ABC12345");
        request.setSpotId("s1");

        assertThatThrownBy(() -> canjeService.validarCanje(request, "socio1"))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void validarCanje_codigoYaUsado_lanza409() {
        when(spotRepository.findById("s1")).thenReturn(Optional.of(spotPropio()));
        when(canjeRepository.findByCodigoIgnoreCase("ABC12345")).thenReturn(Optional.of(
                Canje.builder().estado("USADO").spotId("s1").build()));

        ValidarCanjeRequestDTO request = new ValidarCanjeRequestDTO();
        request.setCodigo("ABC12345");
        request.setSpotId("s1");

        assertThatThrownBy(() -> canjeService.validarCanje(request, "socio1"))
                .isInstanceOf(RecursoNoDisponibleException.class);
    }

    @Test
    void validarCanje_codigoVencido_lanza410() {
        Canje vencido = Canje.builder()
                .estado("VIGENTE")
                .fechaExpiracion(LocalDateTime.now().minusDays(1))
                .spotId("s1")
                .build();
        when(spotRepository.findById("s1")).thenReturn(Optional.of(spotPropio()));
        when(canjeRepository.findByCodigoIgnoreCase("ABC12345")).thenReturn(Optional.of(vencido));

        ValidarCanjeRequestDTO request = new ValidarCanjeRequestDTO();
        request.setCodigo("ABC12345");
        request.setSpotId("s1");

        assertThatThrownBy(() -> canjeService.validarCanje(request, "socio1"))
                .isInstanceOf(RecursoCaducadoException.class);
    }

    @Test
    void validarCanje_promocionYaNoActiva_lanza400() {
        promocionActiva.setActivo(false);
        Canje vigente = Canje.builder()
                .estado("VIGENTE")
                .fechaExpiracion(LocalDateTime.now().plusDays(5))
                .promocionId("p1")
                .spotId("s1")
                .build();
        when(spotRepository.findById("s1")).thenReturn(Optional.of(spotPropio()));
        when(canjeRepository.findByCodigoIgnoreCase("ABC12345")).thenReturn(Optional.of(vigente));
        when(promocionRepository.findById("p1")).thenReturn(Optional.of(promocionActiva));

        ValidarCanjeRequestDTO request = new ValidarCanjeRequestDTO();
        request.setCodigo("ABC12345");
        request.setSpotId("s1");

        assertThatThrownBy(() -> canjeService.validarCanje(request, "socio1"))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void validarCanje_faltaPromocion_lanza404() {
        Canje vigente = Canje.builder()
                .estado("VIGENTE")
                .fechaExpiracion(LocalDateTime.now().plusDays(5))
                .promocionId("p-inexistente")
                .spotId("s1")
                .build();
        when(spotRepository.findById("s1")).thenReturn(Optional.of(spotPropio()));
        when(canjeRepository.findByCodigoIgnoreCase("ABC12345")).thenReturn(Optional.of(vigente));
        when(promocionRepository.findById("p-inexistente")).thenReturn(Optional.empty());

        ValidarCanjeRequestDTO request = new ValidarCanjeRequestDTO();
        request.setCodigo("ABC12345");
        request.setSpotId("s1");

        assertThatThrownBy(() -> canjeService.validarCanje(request, "socio1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- Consultas ---

    @Test
    void listarMios_devuelveSoloLosDelMiembro() {
        Canje canje = Canje.builder()
                .estado("VIGENTE").fechaExpiracion(LocalDateTime.now().plusDays(5))
                .promocionTitulo("50% de descuento en sesión de fotos")
                .spotId("s1").spotNombre("FotoEstudio Bogotá")
                .miembroNombre("miembro1")
                .build();
        when(canjeRepository.findByMiembroNombre(anyString())).thenReturn(List.of(canje));

        List<CanjeResponseDTO> respuesta = canjeService.listarMios("miembro1");

        assertThat(respuesta).hasSize(1);
        assertThat(respuesta.get(0).getMiembroNombre()).isEqualTo("miembro1");
        assertThat(respuesta.get(0).getEstado()).isEqualTo("VIGENTE");
    }

    @Test
    void listarDePromocion_socioAjeno_lanza403() {
        when(promocionRepository.findById("p1")).thenReturn(Optional.of(promocionActiva));

        assertThatThrownBy(() -> canjeService.listarDePromocion("p1", "socio2"))
                .isInstanceOf(AccessForbiddenException.class);
    }

    @Test
    void listarDePromocion_socioDueno_devuelveCanjes() {
        Canje canje = Canje.builder()
                .estado("USADO").spotId("s1")
                .miembroNombre("miembro1").build();
        when(promocionRepository.findById("p1")).thenReturn(Optional.of(promocionActiva));
        when(canjeRepository.findByPromocionId("p1")).thenReturn(List.of(canje));

        List<CanjeResponseDTO> respuesta = canjeService.listarDePromocion("p1", "socio1");

        assertThat(respuesta).hasSize(1);
        assertThat(respuesta.get(0).getEstado()).isEqualTo("USADO");
    }
}