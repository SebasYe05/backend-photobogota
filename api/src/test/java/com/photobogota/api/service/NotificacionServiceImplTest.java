package com.photobogota.api.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.photobogota.api.dto.EnviarNotificacionRequestDTO;
import com.photobogota.api.dto.NotificacionResponseDTO;
import com.photobogota.api.dto.PreferenciasNotificacionDTO;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.mapper.NotificacionMapper;
import com.photobogota.api.model.AlcanceNotificacion;
import com.photobogota.api.model.CanalNotificacion;
import com.photobogota.api.model.Calificacion;
import com.photobogota.api.model.Notificacion;
import com.photobogota.api.model.NotificacionTipo;
import com.photobogota.api.model.PreferenciasNotificacion;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.Spot;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.NotificacionRepository;
import com.photobogota.api.repository.PreferenciasNotificacionRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceImplTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private PreferenciasNotificacionRepository preferenciasNotificacionRepository;

    @Mock
    private UsuarioAuthRepository usuarioAuthRepository;

    @Mock
    private NotificacionMapper notificacionMapper;

    @Mock
    private IEmailService emailService;

    @InjectMocks
    private NotificacionServiceImpl servicio;

    private Notificacion notificacion(String id, String destinatario, Boolean leida) {
        return Notificacion.builder()
                .id(id)
                .destinatarioUsername(destinatario)
                .titulo("Título")
                .mensaje("Mensaje")
                .tipo(NotificacionTipo.SISTEMA)
                .leida(leida)
                .build();
    }

    private PreferenciasNotificacion preferencias(String username) {
        return PreferenciasNotificacion.builder()
                .username(username)
                .notificacionesActivas(true)
                .canalPreferido(CanalNotificacion.APP)
                .build();
    }

    @Test
    void listarMisNotificaciones_soloNoLeidas_usaFiltro() {
        Pageable pageable = PageRequest.of(0, 10);
        Notificacion notificacion = notificacion("n1", "juan", false);
        when(notificacionRepository.findByDestinatarioUsernameAndLeidaOrderByFechaCreacionDesc(
                "juan", false, pageable))
                .thenReturn(new PageImpl<>(List.of(notificacion), pageable, 1));
        NotificacionResponseDTO dto = NotificacionResponseDTO.builder().id("n1").build();
        when(notificacionMapper.toResponse(notificacion)).thenReturn(dto);

        Page<NotificacionResponseDTO> pagina = servicio.listarMisNotificaciones("juan", pageable, true);

        assertThat(pagina.getContent()).containsExactly(dto);
    }

    @Test
    void listarMisNotificaciones_todas_noUsaLeeidas() {
        Pageable pageable = PageRequest.of(0, 10);
        Notificacion notificacion = notificacion("n1", "juan", true);
        when(notificacionRepository.findByDestinatarioUsernameOrderByFechaCreacionDesc("juan", pageable))
                .thenReturn(new PageImpl<>(List.of(notificacion), pageable, 1));
        when(notificacionMapper.toResponse(notificacion))
                .thenReturn(NotificacionResponseDTO.builder().id("n1").build());

        Page<NotificacionResponseDTO> pagina = servicio.listarMisNotificaciones("juan", pageable, false);

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).getId()).isEqualTo("n1");
    }

    @Test
    void contarNoLeidas_devuelveConteo() {
        when(notificacionRepository.countByDestinatarioUsernameAndLeidaFalse("juan")).thenReturn(3L);

        assertThat(servicio.contarNoLeidas("juan")).isEqualTo(3L);
    }

    @Test
    void marcarLeida_noLeida_marcaYGuarda() {
        Notificacion notificacion = notificacion("n1", "juan", false);
        when(notificacionRepository.findById("n1")).thenReturn(Optional.of(notificacion));

        servicio.marcarLeida("n1", "juan");

        assertThat(notificacion.getLeida()).isTrue();
        verify(notificacionRepository).save(notificacion);
    }

    @Test
    void marcarLeida_yaLeida_noGuarda() {
        Notificacion notificacion = notificacion("n1", "juan", true);
        when(notificacionRepository.findById("n1")).thenReturn(Optional.of(notificacion));

        servicio.marcarLeida("n1", "juan");

        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void marcarLeida_deOtraPersona_lanzaResourceNotFound() {
        when(notificacionRepository.findById("n1"))
                .thenReturn(Optional.of(notificacion("n1", "otro", false)));

        assertThatThrownBy(() -> servicio.marcarLeida("n1", "juan"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Notificación no encontrada");
    }

    @Test
    void marcarLeida_noEncontrada_lanzaResourceNotFound() {
        when(notificacionRepository.findById("n1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.marcarLeida("n1", "juan"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void marcarTodasLeidas_marcaYGuardaTodas() {
        Notificacion n1 = notificacion("n1", "juan", false);
        Notificacion n2 = notificacion("n2", "juan", false);
        when(notificacionRepository.findByDestinatarioUsernameAndLeidaFalse("juan"))
                .thenReturn(List.of(n1, n2));

        servicio.marcarTodasLeidas("juan");

        assertThat(n1.getLeida()).isTrue();
        assertThat(n2.getLeida()).isTrue();
        verify(notificacionRepository).saveAll(List.of(n1, n2));
    }

    @Test
    void eliminarNotificacion_propia_elimina() {
        when(notificacionRepository.findById("n1"))
                .thenReturn(Optional.of(notificacion("n1", "juan", false)));

        servicio.eliminarNotificacion("n1", "juan");

        verify(notificacionRepository).delete(notificacion("n1", "juan", false));
    }

    @Test
    void obtenerPreferencias_sinDocumento_usaValoresPorDefecto() {
        when(preferenciasNotificacionRepository.findByUsername("juan")).thenReturn(Optional.empty());

        PreferenciasNotificacionDTO dto = servicio.obtenerPreferencias("juan");

        assertThat(dto.getNotificacionesActivas()).isTrue();
        assertThat(dto.getCanalPreferido()).isEqualTo(CanalNotificacion.APP);
    }

    @Test
    void obtenerPreferencias_conDocumento_mapea() {
        when(preferenciasNotificacionRepository.findByUsername("juan"))
                .thenReturn(Optional.of(preferencias("juan")));

        PreferenciasNotificacionDTO dto = servicio.obtenerPreferencias("juan");

        assertThat(dto.getCanalPreferido()).isEqualTo(CanalNotificacion.APP);
        assertThat(dto.getNotificacionesActivas()).isTrue();
    }

    @Test
    void actualizarPreferencias_existentes_actualizaSoloCamposEnviados() {
        PreferenciasNotificacion prefs = preferencias("juan");
        when(preferenciasNotificacionRepository.findByUsername("juan")).thenReturn(Optional.of(prefs));
        when(preferenciasNotificacionRepository.save(prefs)).thenReturn(prefs);

        PreferenciasNotificacionDTO dto = PreferenciasNotificacionDTO.builder()
                .canalPreferido(CanalNotificacion.EMAIL)
                .build();
        PreferenciasNotificacionDTO resultado = servicio.actualizarPreferencias("juan", dto);

        assertThat(prefs.getCanalPreferido()).isEqualTo(CanalNotificacion.EMAIL);
        assertThat(prefs.getNotificacionesActivas()).isTrue();
        assertThat(resultado.getCanalPreferido()).isEqualTo(CanalNotificacion.EMAIL);
    }

    @Test
    void actualizarPreferencias_sinExistentes_creaNuevas() {
        when(preferenciasNotificacionRepository.findByUsername("juan")).thenReturn(Optional.empty());
        when(preferenciasNotificacionRepository.save(any(PreferenciasNotificacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PreferenciasNotificacionDTO dto = PreferenciasNotificacionDTO.builder()
                .notificacionesActivas(false)
                .categoriasInteres(List.of("Paisaje urbano"))
                .build();
        PreferenciasNotificacionDTO resultado = servicio.actualizarPreferencias("juan", dto);

        assertThat(resultado.getNotificacionesActivas()).isFalse();
        assertThat(resultado.getCategoriasInteres()).containsExactly("Paisaje urbano");

        ArgumentCaptor<PreferenciasNotificacion> captor =
                ArgumentCaptor.forClass(PreferenciasNotificacion.class);
        verify(preferenciasNotificacionRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("juan");
    }

    @Test
    void enviarNotificacionManual_alcanceTodos_noSeNotificaASiMismo() {
        UsuarioAuth admin = UsuarioAuth.builder().nombreUsuario("admin1").build();
        UsuarioAuth user2 = UsuarioAuth.builder().nombreUsuario("user2").build();
        when(usuarioAuthRepository.findAll()).thenReturn(List.of(admin, user2));

        EnviarNotificacionRequestDTO request = EnviarNotificacionRequestDTO.builder()
                .titulo("Anuncio")
                .mensaje("Mantenimiento programado")
                .alcance(AlcanceNotificacion.TODOS)
                .tipo(NotificacionTipo.ANUNCIO_ADMIN)
                .build();
        servicio.enviarNotificacionManual(request, "admin1", "ADMIN");

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        Notificacion guardada = captor.getValue();
        assertThat(guardada.getDestinatarioUsername()).isEqualTo("user2");
        assertThat(guardada.getTipo()).isEqualTo(NotificacionTipo.ANUNCIO_ADMIN);
        assertThat(guardada.getEmisorUsername()).isEqualTo("admin1");
        verify(emailService, never()).enviarCorreoHtml(anyString(), anyString(), anyString());
    }

    @Test
    void enviarNotificacionManual_alcancePorRol_sinRoles_lanzaIllegalArgument() {
        EnviarNotificacionRequestDTO request = EnviarNotificacionRequestDTO.builder()
                .titulo("Anuncio")
                .mensaje("Mensaje")
                .alcance(AlcanceNotificacion.POR_ROL)
                .build();

        assertThatThrownBy(() -> servicio.enviarNotificacionManual(request, "admin1", "ADMIN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos un rol");
    }

    @Test
    void enviarNotificacionManual_alcancePorRol_notificaAUsuariosDelRol() {
        when(usuarioAuthRepository.findByRol(Rol.MOD))
                .thenReturn(List.of(
                        UsuarioAuth.builder().nombreUsuario("mod1").build(),
                        UsuarioAuth.builder().nombreUsuario("mod2").build()));

        EnviarNotificacionRequestDTO request = EnviarNotificacionRequestDTO.builder()
                .titulo("Anuncio")
                .mensaje("Mensaje")
                .alcance(AlcanceNotificacion.POR_ROL)
                .roles(List.of(Rol.MOD))
                .build();
        servicio.enviarNotificacionManual(request, "admin1", "ADMIN");

        verify(notificacionRepository, times(2)).save(any(Notificacion.class));
    }

    @Test
    void enviarNotificacionManual_alcanceUsuariosEspecificos_notificaAUsuarios() {
        EnviarNotificacionRequestDTO request = EnviarNotificacionRequestDTO.builder()
                .titulo("Anuncio")
                .mensaje("Mensaje")
                .alcance(AlcanceNotificacion.USUARIOS_ESPECIFICOS)
                .usernames(List.of("admin1", "user2"))
                .build();
        servicio.enviarNotificacionManual(request, "admin1", "ADMIN");

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        assertThat(captor.getValue().getDestinatarioUsername()).isEqualTo("user2");
    }

    @Test
    void enviarNotificacionManual_destinatarioDesactivado_noGuarda() {
        PreferenciasNotificacion desactivadas = preferencias("user2");
        desactivadas.setNotificacionesActivas(false);
        when(preferenciasNotificacionRepository.findByUsername("user2"))
                .thenReturn(Optional.of(desactivadas));

        EnviarNotificacionRequestDTO request = EnviarNotificacionRequestDTO.builder()
                .titulo("Anuncio")
                .mensaje("Mensaje")
                .alcance(AlcanceNotificacion.USUARIOS_ESPECIFICOS)
                .usernames(List.of("user2"))
                .build();
        servicio.enviarNotificacionManual(request, "admin1", "ADMIN");

        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void notificarNuevoSpot_conCanalEmail_enviaCorreoYGuarda() {
        Spot spot = new Spot();
        spot.setId("s1");
        spot.setNombre("Plaza de Bolívar");
        spot.setLocalidad("La Candelaria");
        spot.setCategoria("Patrimonio");
        spot.setCreadorUsername("juan");

        PreferenciasNotificacion interesado = preferencias("user2");
        interesado.setCanalPreferido(CanalNotificacion.EMAIL);
        when(preferenciasNotificacionRepository
                .findByLocalidadesInteresContainingOrCategoriasInteresContaining("La Candelaria", "Patrimonio"))
                .thenReturn(List.of(interesado));
        when(usuarioAuthRepository.findByNombreUsuario("user2"))
                .thenReturn(Optional.of(UsuarioAuth.builder()
                        .nombreUsuario("user2")
                        .email("user2@mail.com")
                        .build()));

        servicio.notificarNuevoSpot(spot);

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        Notificacion guardada = captor.getValue();
        assertThat(guardada.getDestinatarioUsername()).isEqualTo("user2");
        assertThat(guardada.getTipo()).isEqualTo(NotificacionTipo.NUEVO_SPOT_INTERES);
        assertThat(guardada.getSpotId()).isEqualTo("s1");
        verify(emailService).enviarCorreoHtml(eq("user2@mail.com"), eq("Nuevo spot en La Candelaria"), anyString());
    }

    @Test
    void notificarNuevoSpot_creadorEnInteresados_noSeNotifica() {
        Spot spot = new Spot();
        spot.setNombre("Plaza de Bolívar");
        spot.setLocalidad("La Candelaria");
        spot.setCategoria("Patrimonio");
        spot.setCreadorUsername("user2");

        when(preferenciasNotificacionRepository
                .findByLocalidadesInteresContainingOrCategoriasInteresContaining("La Candelaria", "Patrimonio"))
                .thenReturn(List.of(preferencias("user2")));

        servicio.notificarNuevoSpot(spot);

        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void notificarNuevaResena_destinatarioDistinto_creaNotificacion() {
        Spot spot = new Spot();
        spot.setId("s1");
        spot.setNombre("Plaza de Bolívar");
        spot.setCreadorUsername("socio1");
        Spot.Resena resena = new Spot.Resena();
        resena.setRating(4);

        servicio.notificarNuevaResena(spot, resena, "juan");

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        Notificacion guardada = captor.getValue();
        assertThat(guardada.getDestinatarioUsername()).isEqualTo("socio1");
        assertThat(guardada.getTipo()).isEqualTo(NotificacionTipo.NUEVA_RESENA);
        assertThat(guardada.getTitulo()).isEqualTo("Nueva reseña en tu spot");
        assertThat(guardada.getEmisorUsername()).isEqualTo("juan");
    }

    @Test
    void notificarNuevaResena_autoResena_noNotifica() {
        Spot spot = new Spot();
        spot.setCreadorUsername("juan");
        Spot.Resena resena = new Spot.Resena();
        resena.setRating(5);

        servicio.notificarNuevaResena(spot, resena, "juan");

        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void notificarNuevaCalificacion_ignoraAutoCalificacion() {
        Spot spot = new Spot();
        spot.setCreadorUsername("juan");
        Calificacion calificacion = Calificacion.builder().estrellas(5).build();

        servicio.notificarNuevaCalificacion(spot, calificacion, "juan");

        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void notificarSistema_creaYGuardaConEmisorSistema() {
        servicio.notificarSistema("juan", "Aviso", "Mensaje del sistema");

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        Notificacion guardada = captor.getValue();
        assertThat(guardada.getDestinatarioUsername()).isEqualTo("juan");
        assertThat(guardada.getTipo()).isEqualTo(NotificacionTipo.SISTEMA);
        assertThat(guardada.getEmisorUsername()).isEqualTo("sistema");
        assertThat(guardada.getLeida()).isFalse();
    }

    @Test
    void notificarSubidaNivel_mencionaNivelAlcanzado() {
        servicio.notificarSubidaNivel("juan", 3);

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        Notificacion guardada = captor.getValue();
        assertThat(guardada.getTitulo()).isEqualTo("¡Subiste de nivel!");
        assertThat(guardada.getMensaje()).contains("nivel 3");
        assertThat(guardada.getTipo()).isEqualTo(NotificacionTipo.SISTEMA);
    }
}