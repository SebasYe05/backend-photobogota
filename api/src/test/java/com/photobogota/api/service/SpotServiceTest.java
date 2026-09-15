package com.photobogota.api.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.photobogota.api.dto.CrearSpotRequestDTO;
import com.photobogota.api.dto.ResenaRequestDTO;
import com.photobogota.api.dto.SpotResumenDTO;
import com.photobogota.api.dto.SpotResponseDTO;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.mapper.SpotMapper;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.Spot;
import com.photobogota.api.model.TipoContenidoModerado;
import com.photobogota.api.model.TipoPuntos;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.SpotRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotServiceTest {

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private SpotMapper spotMapper;

    @Mock
    private UsuarioAuthRepository usuarioAuthRepository;

    @Mock
    private INotificacionService notificacionService;

    @Mock
    private IPuntosService puntosService;

    @Mock
    private IFiltroContenidoService filtroContenidoService;

    @Mock
    private PromocionService promocionService;

    @InjectMocks
    private SpotService spotService;

    private Spot spotDeEjemplo(String id, String nombre) {
        Spot spot = new Spot();
        spot.setId(id);
        spot.setNombre(nombre);
        spot.setTipo("SPOT");
        spot.setCreadorRol("MOD");
        spot.setImagenes(List.of("/spots/" + id + ".jpg"));
        return spot;
    }

    private SpotResumenDTO resumenDeEjemplo(String id) {
        SpotResumenDTO dto = new SpotResumenDTO();
        dto.setId(id);
        dto.setNombre("Spot " + id);
        return dto;
    }

    private SpotResponseDTO responseDeEjemplo(String id) {
        SpotResponseDTO dto = new SpotResponseDTO();
        dto.setId(id);
        dto.setNombre("Spot " + id);
        return dto;
    }

    private CrearSpotRequestDTO requestCrear() {
        CrearSpotRequestDTO request = new CrearSpotRequestDTO();
        request.setNombre("Parque Central");
        request.setCategoria("Parque");
        request.setLocalidad("Kennedy");
        request.setDescripcion("Un gran parque para fotos");
        return request;
    }

    @Test
    void obtenerTodos_sinFiltros_devuelveTodosYEnriqueceConPromociones() {
        when(spotRepository.findAll()).thenReturn(
                List.of(spotDeEjemplo("s1", "Parque Simón Bolívar"), spotDeEjemplo("s2", "Museo Nacional")));
        SpotResumenDTO dto1 = resumenDeEjemplo("s1");
        SpotResumenDTO dto2 = resumenDeEjemplo("s2");
        when(spotMapper.toResumenList(any())).thenReturn(List.of(dto1, dto2));
        when(promocionService.obtenerSpotIdsConPromocionActiva()).thenReturn(Set.of("s1"));

        List<SpotResumenDTO> resultado = spotService.obtenerTodos(null, null, null, null, null, null);

        assertThat(resultado).hasSize(2);
        assertThat(dto1.getTienePromocion()).isTrue();
        assertThat(dto2.getTienePromocion()).isFalse();
    }

    @Test
    void obtenerTodos_conCategoriaYLocalidad_usaEseMetodo() {
        when(spotRepository.findByCategoriaAndLocalidad("Parque", "Kennedy"))
                .thenReturn(List.of(spotDeEjemplo("s1", "Parque")));
        when(spotMapper.toResumenList(any())).thenReturn(List.of(resumenDeEjemplo("s1")));
        when(promocionService.obtenerSpotIdsConPromocionActiva()).thenReturn(Set.of());

        List<SpotResumenDTO> resultado = spotService.obtenerTodos("Parque", "Kennedy", null, null, null, null);

        assertThat(resultado).hasSize(1);
    }

    @Test
    void obtenerTodos_filtroTipo_normalizaLocalesDeSocio() {
        Spot localAntiguo = spotDeEjemplo("s1", "Caldos");
        localAntiguo.setTipo(null);
        localAntiguo.setCreadorRol("SOCIO");
        Spot regular = spotDeEjemplo("s2", "Mirador");
        when(spotRepository.findAll()).thenReturn(List.of(localAntiguo, regular));
        when(spotMapper.toResumenList(any())).thenReturn(List.of(resumenDeEjemplo("s1")));
        when(promocionService.obtenerSpotIdsConPromocionActiva()).thenReturn(Set.of());

        List<SpotResumenDTO> resultado = spotService.obtenerTodos(null, null, "LOCAL", null, null, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo("s1");
    }

    @Test
    void obtenerTodos_conFiltroMios_soloDevuelveLosSuEstablecimientos() {
        Spot propio = spotDeEjemplo("s1", "Caldos");
        propio.setCreadorUsername("socio1");
        Spot ajeno = spotDeEjemplo("s2", "Museo");
        ajeno.setCreadorUsername("socio2");
        when(spotRepository.findAll()).thenReturn(List.of(propio, ajeno));
        when(spotMapper.toResumenList(any())).thenReturn(List.of(resumenDeEjemplo("s1")));
        when(promocionService.obtenerSpotIdsConPromocionActiva()).thenReturn(Set.of());

        List<SpotResumenDTO> resultado = spotService.obtenerTodos(null, null, null, null, true, "socio1");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo("s1");
    }

    @Test
    void obtenerTodos_filtroPorNombre_esInsensibleAMayusculas() {
        when(spotRepository.findAll()).thenReturn(
                List.of(spotDeEjemplo("s1", "Parque Simón Bolívar"), spotDeEjemplo("s2", "Museo Nacional")));
        when(spotMapper.toResumenList(any())).thenReturn(List.of(resumenDeEjemplo("s1")));
        when(promocionService.obtenerSpotIdsConPromocionActiva()).thenReturn(Set.of());

        List<SpotResumenDTO> resultado = spotService.obtenerTodos(null, null, null, "simón", null, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo("s1");
    }

    @Test
    void obtenerPorId_existente_siempreRolPresenteDevuelveDetalle() {
        Spot spot = spotDeEjemplo("s1", "Parque");
        spot.setCreadorRol("SOCIO");
        when(spotRepository.findById("s1")).thenReturn(Optional.of(spot));
        SpotResponseDTO response = responseDeEjemplo("s1");
        when(spotMapper.toResponse(spot)).thenReturn(response);
        when(promocionService.tienePromocionActiva("s1")).thenReturn(true);

        SpotResponseDTO resultado = spotService.obtenerPorId("s1");

        assertThat(resultado.getRol()).isEqualTo("SOCIO");
        assertThat(resultado.getTienePromocion()).isTrue();
    }

    @Test
    void obtenerPorId_sinRolConUsername_resuelveElRolDesdeAuthYGuarda() {
        Spot spot = spotDeEjemplo("s1", "Parque");
        spot.setCreadorRol(null);
        spot.setCreadorUsername("creador1");
        when(spotRepository.findById("s1")).thenReturn(Optional.of(spot));
        SpotResponseDTO response = responseDeEjemplo("s1");
        when(spotMapper.toResponse(spot)).thenReturn(response);
        when(promocionService.tienePromocionActiva("s1")).thenReturn(false);
        when(usuarioAuthRepository.findByEmailOrNombreUsuario("creador1", "creador1"))
                .thenReturn(Optional.of(UsuarioAuth.builder().nombreUsuario("creador1").rol(Rol.MIEMBRO).build()));

        SpotResponseDTO resultado = spotService.obtenerPorId("s1");

        assertThat(resultado.getRol()).isEqualTo("MIEMBRO");
        assertThat(spot.getCreadorRol()).isEqualTo("MIEMBRO");
        verify(spotRepository).save(spot);
    }

    @Test
    void obtenerPorId_sinRolSinUsername_dejaElRolNulo() {
        Spot spot = spotDeEjemplo("s1", "Parque");
        spot.setCreadorRol(null);
        spot.setCreadorUsername(null);
        when(spotRepository.findById("s1")).thenReturn(Optional.of(spot));
        SpotResponseDTO response = responseDeEjemplo("s1");
        when(spotMapper.toResponse(spot)).thenReturn(response);
        when(promocionService.tienePromocionActiva("s1")).thenReturn(false);

        SpotResponseDTO resultado = spotService.obtenerPorId("s1");

        assertThat(resultado.getRol()).isNull();
        verify(spotRepository, never()).save(any());
    }

    @Test
    void obtenerPorId_inexistente_lanzaResourceNotFound() {
        when(spotRepository.findById("s-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spotService.obtenerPorId("s-x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crearSpot_socio_construyeUnLocal() {
        when(spotRepository.save(any(Spot.class))).thenAnswer(inv -> inv.getArgument(0));
        SpotResponseDTO response = responseDeEjemplo("s1");
        response.setRol("SOCIO");
        when(spotMapper.toResponse(any(Spot.class))).thenReturn(response);
        when(usuarioAuthRepository.findByEmailOrNombreUsuario("socio1", "socio1"))
                .thenReturn(Optional.of(UsuarioAuth.builder().nombreUsuario("socio1").rol(Rol.SOCIO).build()));

        SpotResponseDTO resultado = spotService.crearSpot(requestCrear(), "socio1", "SOCIO");

        org.mockito.ArgumentCaptor<Spot> captor = org.mockito.ArgumentCaptor.forClass(Spot.class);
        verify(spotRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo("LOCAL");
        assertThat(captor.getValue().getCreadorRol()).isEqualTo("SOCIO");
        assertThat(resultado.getRol()).isEqualTo("SOCIO");
        verify(notificacionService).notificarNuevoSpot(captor.getValue());
    }

    @Test
    void crearSpot_miembro_noPuedeRegistrarLocal() {
        when(spotRepository.save(any(Spot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(spotMapper.toResponse(any(Spot.class))).thenReturn(responseDeEjemplo("s1"));
        when(usuarioAuthRepository.findByEmailOrNombreUsuario("miembro1", "miembro1"))
                .thenReturn(Optional.of(UsuarioAuth.builder().nombreUsuario("miembro1").rol(Rol.MIEMBRO).build()));

        CrearSpotRequestDTO request = requestCrear();
        request.setTipo("LOCAL");

        spotService.crearSpot(request, "miembro1", "MIEMBRO");

        org.mockito.ArgumentCaptor<Spot> captor = org.mockito.ArgumentCaptor.forClass(Spot.class);
        verify(spotRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo("SPOT");
    }

    @Test
    void crearSpot_miembro_otorgaPuntos() {
        when(spotRepository.save(any(Spot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(spotMapper.toResponse(any(Spot.class))).thenReturn(responseDeEjemplo("s1"));
        when(usuarioAuthRepository.findByEmailOrNombreUsuario("miembro1", "miembro1"))
                .thenReturn(Optional.of(UsuarioAuth.builder().nombreUsuario("miembro1").rol(Rol.MIEMBRO).build()));

        spotService.crearSpot(requestCrear(), "miembro1", "MIEMBRO");

        verify(puntosService).sumarPuntos(eq("miembro1"), eq(TipoPuntos.CREAR_SPOT), any());
    }

    @Test
    void crearSpot_socio_noOtorgaPuntos() {
        when(spotRepository.save(any(Spot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(spotMapper.toResponse(any(Spot.class))).thenReturn(responseDeEjemplo("s1"));
        when(usuarioAuthRepository.findByEmailOrNombreUsuario("socio1", "socio1"))
                .thenReturn(Optional.of(UsuarioAuth.builder().nombreUsuario("socio1").rol(Rol.SOCIO).build()));

        spotService.crearSpot(requestCrear(), "socio1", "SOCIO");

        verify(puntosService, never()).sumarPuntos(any(), any(), any());
    }

    @Test
    void crearSpot_validaContenidoDelNombreYLaDescripcion() {
        when(spotRepository.save(any(Spot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(spotMapper.toResponse(any(Spot.class))).thenReturn(responseDeEjemplo("s1"));
        when(usuarioAuthRepository.findByEmailOrNombreUsuario("miembro1", "miembro1"))
                .thenReturn(Optional.of(UsuarioAuth.builder().nombreUsuario("miembro1").rol(Rol.MIEMBRO).build()));

        spotService.crearSpot(requestCrear(), "miembro1", "MIEMBRO");

        verify(filtroContenidoService).validarContenido(eq("miembro1"), eq(TipoContenidoModerado.SPOT_NOMBRE), any());
        verify(filtroContenidoService).validarContenido(eq("miembro1"), eq(TipoContenidoModerado.SPOT_DESCRIPCION), any());
    }

    @Test
    void agregarResena_agregaResenaYCalculaRating() {
        Spot spot = spotDeEjemplo("s1", "Parque");
        spot.setCreadorRol("SOCIO");
        spot.setCreadorUsername("socio1");
        when(spotRepository.findById("s1")).thenReturn(Optional.of(spot));
        when(spotRepository.save(any(Spot.class))).thenAnswer(inv -> inv.getArgument(0));
        SpotResponseDTO response = responseDeEjemplo("s1");
        when(spotMapper.toResponse(any(Spot.class))).thenReturn(response);
        when(promocionService.tienePromocionActiva("s1")).thenReturn(true);

        ResenaRequestDTO request = new ResenaRequestDTO();
        request.setRating(4);
        request.setComentario("Buen lugar");

        SpotResponseDTO resultado = spotService.agregarResena("s1", request, "miembro1");

        assertThat(spot.getTotalResenas()).isEqualTo(1);
        assertThat(spot.getRating()).isEqualTo(4.0);
        assertThat(spot.getResenas().get(0).getId()).isNotBlank();
        assertThat(resultado.getRol()).isEqualTo("SOCIO");
        assertThat(resultado.getTienePromocion()).isTrue();
        verify(notificacionService).notificarNuevaResena(eq(spot), any(), eq("miembro1"));
    }

    @Test
    void agregarResena_promediaConLasExistentes() {
        Spot spot = spotDeEjemplo("s1", "Parque");
        Spot.Resena existente = new Spot.Resena();
        existente.setRating(5);
        spot.getResenas().add(existente);
        when(spotRepository.findById("s1")).thenReturn(Optional.of(spot));
        when(spotRepository.save(any(Spot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(spotMapper.toResponse(any(Spot.class))).thenReturn(responseDeEjemplo("s1"));
        when(promocionService.tienePromocionActiva("s1")).thenReturn(false);

        ResenaRequestDTO request = new ResenaRequestDTO();
        request.setRating(3);
        request.setComentario("Regular");

        spotService.agregarResena("s1", request, "miembro1");

        assertThat(spot.getTotalResenas()).isEqualTo(2);
        assertThat(spot.getRating()).isEqualTo(4.0);
    }

    @Test
    void agregarResena_spotInexistente_lanzaResourceNotFound() {
        when(spotRepository.findById("s-x")).thenReturn(Optional.empty());

        ResenaRequestDTO request = new ResenaRequestDTO();
        request.setRating(4);
        request.setComentario("Buen lugar");

        assertThatThrownBy(() -> spotService.agregarResena("s-x", request, "miembro1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}