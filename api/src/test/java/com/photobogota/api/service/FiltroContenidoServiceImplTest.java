package com.photobogota.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.photobogota.api.dto.PalabraProhibidaDTO;
import com.photobogota.api.dto.RegistroModeracionDTO;
import com.photobogota.api.dto.ResolverApelacionRequestDTO;
import com.photobogota.api.dto.SancionDTO;
import com.photobogota.api.exception.ContenidoInapropiadoException;
import com.photobogota.api.exception.OperacionInvalidaException;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.model.AccionModeracion;
import com.photobogota.api.model.EstadoApelacion;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.PalabraProhibida;
import com.photobogota.api.model.RegistroModeracion;
import com.photobogota.api.model.Sancion;
import com.photobogota.api.model.TipoContenidoModerado;
import com.photobogota.api.model.TipoPalabra;
import com.photobogota.api.model.TipoSancion;
import com.photobogota.api.model.Usuario;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.PalabraProhibidaRepository;
import com.photobogota.api.repository.RegistroModeracionRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FiltroContenidoServiceImplTest {

    @Mock
    private PalabraProhibidaRepository palabraProhibidaRepository;

    @Mock
    private RegistroModeracionRepository registroModeracionRepository;

    @Mock
    private UsuarioAuthRepository usuarioAuthRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private INotificacionService notificacionService;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private FiltroContenidoServiceImpl servicio;

    private static final ObjectId USUARIO_ID = new ObjectId();
    private static final String NOMBRE = "juan";

    private UsuarioAuth auth() {
        return UsuarioAuth.builder().id(USUARIO_ID).nombreUsuario(NOMBRE).build();
    }

    private Miembro usuario() {
        return Miembro.builder().id(USUARIO_ID).contadorInfracciones(0).build();
    }

    private Miembro usuarioConSancion(TipoSancion tipo, LocalDateTime expiracion) {
        return Miembro.builder()
                .id(USUARIO_ID)
                .contadorInfracciones(2)
                .sancion(Sancion.builder()
                        .tipo(tipo)
                        .fechaInicio(LocalDateTime.now().minusDays(1))
                        .fechaExpiracion(expiracion)
                        .motivo("Motivo de prueba")
                        .impuestaPor("AUTO")
                        .build())
                .build();
    }

    private void stubUsuario(Usuario usuario) {
        when(usuarioAuthRepository.findByNombreUsuario(NOMBRE)).thenReturn(Optional.of(auth()));
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
    }

    @Test
    void verificarPermisoPublicar_sinSancion_noLanzaNiGuarda() {
        stubUsuario(usuario());

        servicio.verificarPermisoPublicar(NOMBRE);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void verificarPermisoPublicar_sancionNotificacion_noBloquea() {
        stubUsuario(usuarioConSancion(TipoSancion.NOTIFICACION, LocalDateTime.now().plusDays(1)));

        servicio.verificarPermisoPublicar(NOMBRE);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void verificarPermisoPublicar_sancionExpirada_limpiaSancionYGuarda() {
        Miembro usuario = usuarioConSancion(TipoSancion.MUTE, LocalDateTime.now().minusDays(1));
        stubUsuario(usuario);

        servicio.verificarPermisoPublicar(NOMBRE);

        assertThat(usuario.getSancion()).isNull();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void verificarPermisoPublicar_sancionMuteActiva_lanzaContenidoInapropiado() {
        Miembro usuario = usuarioConSancion(TipoSancion.MUTE, LocalDateTime.now().plusDays(5));
        stubUsuario(usuario);

        assertThatThrownBy(() -> servicio.verificarPermisoPublicar(NOMBRE))
                .isInstanceOf(ContenidoInapropiadoException.class)
                .hasMessageContaining("silencio activo");
    }

    @Test
    void validarContenido_sinPalabrasProhibidas_noSanciona() {
        stubUsuario(usuario());
        when(palabraProhibidaRepository.findByActivoTrue()).thenReturn(List.of());

        servicio.validarContenido(NOMBRE, TipoContenidoModerado.SPOT_NOMBRE, "Un spot perfectamente normal");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void validarContenido_primeraInfraccion_aplicaNotificacion() {
        Miembro usuario = usuario();
        stubUsuario(usuario);
        PalabraProhibida regla = PalabraProhibida.builder()
                .texto("tonto").tipo(TipoPalabra.PALABRA).activo(true).build();
        when(palabraProhibidaRepository.findByActivoTrue()).thenReturn(List.of(regla));

        assertThatThrownBy(() -> servicio.validarContenido(
                NOMBRE, TipoContenidoModerado.RESENA, "El autor es muy tonto"))
                .isInstanceOf(ContenidoInapropiadoException.class)
                .hasMessageContaining("notificación");

        assertThat(usuario.getContadorInfracciones()).isEqualTo(1);
        assertThat(usuario.getSancion().getTipo()).isEqualTo(TipoSancion.NOTIFICACION);
        verify(usuarioRepository).save(usuario);
        verify(notificacionService).notificarSistema(eq(NOMBRE), eq("Aviso de moderación"), anyString());

        ArgumentCaptor<RegistroModeracion> captor = ArgumentCaptor.forClass(RegistroModeracion.class);
        verify(registroModeracionRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<RegistroModeracion> guardados = captor.getAllValues();
        assertThat(guardados.get(0).getAccion()).isEqualTo(AccionModeracion.DETECCION);
        assertThat(guardados.get(1).getAccion()).isEqualTo(AccionModeracion.NOTIFICACION);
        assertThat(guardados.get(1).getOrigen()).isEqualTo("AUTO");
    }

    @Test
    void validarContenido_segundaInfraccion_aplicaMuteConExpiracion() {
        Miembro usuario = Miembro.builder().id(USUARIO_ID).contadorInfracciones(1).build();
        stubUsuario(usuario);
        PalabraProhibida regla = PalabraProhibida.builder()
                .texto("tonto").tipo(TipoPalabra.PALABRA).activo(true).build();
        when(palabraProhibidaRepository.findByActivoTrue()).thenReturn(List.of(regla));

        assertThatThrownBy(() -> servicio.validarContenido(
                NOMBRE, TipoContenidoModerado.RESENA, "El autor es muy tonto"))
                .isInstanceOf(ContenidoInapropiadoException.class)
                .hasMessageContaining("silencio");

        assertThat(usuario.getSancion().getTipo()).isEqualTo(TipoSancion.MUTE);
        assertThat(usuario.getSancion().getFechaExpiracion()).isNotNull();
        verify(notificacionService).notificarSistema(eq(NOMBRE), eq("Silencio temporal"), anyString());
    }

    @Test
    void validarContenido_cuartaInfraccion_aplicaBanIndefinido() {
        Miembro usuario = Miembro.builder().id(USUARIO_ID).contadorInfracciones(3).build();
        stubUsuario(usuario);
        PalabraProhibida regla = PalabraProhibida.builder()
                .texto("tonto").tipo(TipoPalabra.PALABRA).activo(true).build();
        when(palabraProhibidaRepository.findByActivoTrue()).thenReturn(List.of(regla));

        assertThatThrownBy(() -> servicio.validarContenido(
                NOMBRE, TipoContenidoModerado.RESENA, "El autor es muy tonto"))
                .isInstanceOf(ContenidoInapropiadoException.class)
                .hasMessageContaining("indefinida");

        assertThat(usuario.getSancion().getTipo()).isEqualTo(TipoSancion.BAN);
        assertThat(usuario.getSancion().getFechaExpiracion()).isNull();
        verify(notificacionService).notificarSistema(eq(NOMBRE), eq("Cuenta suspendida"), anyString());
    }

    @Test
    void analizar_detectaPalabraCompletaIgnorandoTildes() {
        PalabraProhibida regla = PalabraProhibida.builder()
                .texto("tonto").tipo(TipoPalabra.PALABRA).activo(true).build();
        when(palabraProhibidaRepository.findByActivoTrue()).thenReturn(List.of(regla));

        assertThat(servicio.analizar("Eres Tónto")).containsExactly("tonto");
    }

    @Test
    void analizar_detectaFrasePorSubcadena() {
        PalabraProhibida regla = PalabraProhibida.builder()
                .texto("venta de").tipo(TipoPalabra.FRASE).activo(true).build();
        when(palabraProhibidaRepository.findByActivoTrue()).thenReturn(List.of(regla));

        assertThat(servicio.analizar("TE OFREZCO VENTA DE FOTOS")).containsExactly("venta de");
    }

    @Test
    void analizar_ignoraPalabraDentroDeExcepcion() {
        PalabraProhibida regla = PalabraProhibida.builder()
                .texto("capitalista")
                .tipo(TipoPalabra.PALABRA)
                .activo(true)
                .excepciones(List.of("capitalista de barrio"))
                .build();
        when(palabraProhibidaRepository.findByActivoTrue()).thenReturn(List.of(regla));

        assertThat(servicio.analizar("el capitalista de barrio gastó su dinero")).isEmpty();
    }

    @Test
    void obtenerSancionActual_sinSancion_noBloquea() {
        stubUsuario(usuario());

        SancionDTO dto = servicio.obtenerSancionActual(NOMBRE);

        assertThat(dto.getTipo()).isNull();
        assertThat(dto.getBloqueaPublicacion()).isFalse();
        assertThat(dto.getPuedeApelar()).isFalse();
        assertThat(dto.getContadorInfracciones()).isEqualTo(0);
    }

    @Test
    void obtenerSancionActual_sancionBan_permiteApelar() {
        stubUsuario(usuarioConSancion(TipoSancion.BAN, null));

        SancionDTO dto = servicio.obtenerSancionActual(NOMBRE);

        assertThat(dto.getTipo()).isEqualTo(TipoSancion.BAN);
        assertThat(dto.getBloqueaPublicacion()).isTrue();
        assertThat(dto.getPuedeApelar()).isTrue();
    }

    @Test
    void apelarBan_conSancionActiva_creaApelacion() {
        stubUsuario(usuarioConSancion(TipoSancion.BAN, null));
        RegistroModeracion registro = RegistroModeracion.builder()
                .id("r1")
                .usuarioId(USUARIO_ID.toHexString())
                .nombreUsuario(NOMBRE)
                .accion(AccionModeracion.BAN)
                .build();
        when(registroModeracionRepository.findByUsuarioIdOrderByFechaDesc(USUARIO_ID.toHexString()))
                .thenReturn(List.of(registro));

        servicio.apelarBan(NOMBRE, "No fui yo");

        assertThat(registro.getEstadoApelacion()).isEqualTo(EstadoApelacion.PENDIENTE);
        assertThat(registro.getMotivoApelacion()).isEqualTo("No fui yo");
        assertThat(registro.getFechaApelacion()).isNotNull();

        ArgumentCaptor<RegistroModeracion> captor = ArgumentCaptor.forClass(RegistroModeracion.class);
        verify(registroModeracionRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0)).isSameAs(registro);
        assertThat(captor.getAllValues().get(1).getAccion()).isEqualTo(AccionModeracion.DETECCION);
    }

    @Test
    void apelarBan_sinSancion_lanzaOperacionInvalida() {
        stubUsuario(usuario());

        assertThatThrownBy(() -> servicio.apelarBan(NOMBRE, "No fui yo"))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("Solo puedes apelar");
    }

    @Test
    void apelarBan_sancionNoApelable_lanzaOperacionInvalida() {
        stubUsuario(usuarioConSancion(TipoSancion.NOTIFICACION, LocalDateTime.now().plusDays(1)));

        assertThatThrownBy(() -> servicio.apelarBan(NOMBRE, "No fui yo"))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("Solo puedes apelar");
    }

    @Test
    void listarPalabras_devuelveTodasOrdenadas() {
        PalabraProhibida regla = PalabraProhibida.builder()
                .id("p1").texto("malo").tipo(TipoPalabra.PALABRA).activo(true).build();
        when(palabraProhibidaRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(List.of(regla));

        List<PalabraProhibidaDTO> dto = servicio.listarPalabras();

        assertThat(dto).hasSize(1);
        assertThat(dto.get(0).getTexto()).isEqualTo("malo");
    }

    @Test
    void crearPalabra_textoObligatorio_lanzaOperacionInvalida() {
        assertThatThrownBy(() -> servicio.crearPalabra(
                PalabraProhibidaDTO.builder().build(), "admin1"))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("obligatorio");
    }

    @Test
    void crearPalabra_valida_asignaValoresPorDefecto() {
        PalabraProhibida guardada = PalabraProhibida.builder()
                .id("p2")
                .texto("malo")
                .tipo(TipoPalabra.PALABRA)
                .activo(true)
                .categoria("general")
                .creadoPor("admin1")
                .fechaCreacion(LocalDateTime.now())
                .build();
        when(palabraProhibidaRepository.save(any(PalabraProhibida.class))).thenReturn(guardada);

        PalabraProhibidaDTO dto = servicio.crearPalabra(
                PalabraProhibidaDTO.builder().texto("malo").build(), "admin1");

        assertThat(dto.getTipo()).isEqualTo(TipoPalabra.PALABRA);
        assertThat(dto.getActivo()).isTrue();
        assertThat(dto.getCreadoPor()).isEqualTo("admin1");
    }

    @Test
    void actualizarPalabra_noEncontrada_lanzaResourceNotFound() {
        when(palabraProhibidaRepository.findById("p1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizarPalabra("p1",
                PalabraProhibidaDTO.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Palabra prohibida no encontrada");
    }

    @Test
    void togglePalabra_alternaActivo() {
        PalabraProhibida regla = PalabraProhibida.builder()
                .id("p1").texto("malo").tipo(TipoPalabra.PALABRA).activo(true).build();
        when(palabraProhibidaRepository.findById("p1")).thenReturn(Optional.of(regla));
        when(palabraProhibidaRepository.save(regla)).thenReturn(regla);

        PalabraProhibidaDTO dto = servicio.togglePalabra("p1");

        assertThat(dto.getActivo()).isFalse();
    }

    @Test
    void eliminarPalabra_noExiste_lanzaResourceNotFound() {
        when(palabraProhibidaRepository.existsById("p1")).thenReturn(false);

        assertThatThrownBy(() -> servicio.eliminarPalabra("p1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void eliminarPalabra_existe_elimina() {
        when(palabraProhibidaRepository.existsById("p1")).thenReturn(true);

        servicio.eliminarPalabra("p1");

        verify(palabraProhibidaRepository).deleteById("p1");
    }

    @Test
    void listarHistorial_devuelvePaginaDesdeMongoTemplate() {
        Pageable pageable = PageRequest.of(0, 10);
        RegistroModeracion registro = RegistroModeracion.builder()
                .id("r1")
                .usuarioId(USUARIO_ID.toHexString())
                .nombreUsuario(NOMBRE)
                .accion(AccionModeracion.MUTE)
                .build();
        when(mongoTemplate.count(any(Query.class), eq(RegistroModeracion.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(RegistroModeracion.class)))
                .thenReturn(List.of(registro));

        Page<RegistroModeracionDTO> pagina = servicio.listarHistorial(
                AccionModeracion.MUTE, NOMBRE, TipoContenidoModerado.RESENA,
                LocalDateTime.now().minusDays(1), LocalDateTime.now(), pageable);

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).getAccion()).isEqualTo(AccionModeracion.MUTE);
    }

    @Test
    void listarApelacionesPendientes_filtraPorAccionesApelables() {
        RegistroModeracion registro = RegistroModeracion.builder()
                .id("r1")
                .usuarioId(USUARIO_ID.toHexString())
                .accion(AccionModeracion.BAN)
                .estadoApelacion(EstadoApelacion.PENDIENTE)
                .build();
        when(registroModeracionRepository.findByAccionInAndEstadoApelacionOrderByFechaDesc(
                anyList(), eq(EstadoApelacion.PENDIENTE)))
                .thenReturn(List.of(registro));

        List<RegistroModeracionDTO> apelaciones = servicio.listarApelacionesPendientes();

        assertThat(apelaciones).hasSize(1);
        assertThat(apelaciones.get(0).getEstadoApelacion()).isEqualTo(EstadoApelacion.PENDIENTE);
    }

    @Test
    void resolverApelacion_aprobada_levantaSancionYNotifica() {
        RegistroModeracion registro = RegistroModeracion.builder()
                .id("r1")
                .usuarioId(USUARIO_ID.toHexString())
                .nombreUsuario(NOMBRE)
                .accion(AccionModeracion.BAN)
                .estadoApelacion(EstadoApelacion.PENDIENTE)
                .build();
        when(registroModeracionRepository.findById("r1")).thenReturn(Optional.of(registro));
        Miembro usuario = usuarioConSancion(TipoSancion.BAN, null);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        ResolverApelacionRequestDTO request = ResolverApelacionRequestDTO.builder()
                .aprobar(true).respuesta("Apelación aceptada").build();
        RegistroModeracionDTO dto = servicio.resolverApelacion("r1", request, "admin1");

        assertThat(registro.getEstadoApelacion()).isEqualTo(EstadoApelacion.APROBADA);
        assertThat(registro.getRevisadaPor()).isEqualTo("admin1");
        assertThat(dto.getEstadoApelacion()).isEqualTo(EstadoApelacion.APROBADA);
        assertThat(usuario.getSancion()).isNull();
        verify(usuarioRepository).save(usuario);
        verify(notificacionService).notificarSistema(eq(NOMBRE), eq("Apelación aprobada"), anyString());
    }

    @Test
    void resolverApelacion_rechazada_conservaSancion() {
        RegistroModeracion registro = RegistroModeracion.builder()
                .id("r1")
                .usuarioId(USUARIO_ID.toHexString())
                .nombreUsuario(NOMBRE)
                .accion(AccionModeracion.BAN)
                .estadoApelacion(EstadoApelacion.PENDIENTE)
                .build();
        when(registroModeracionRepository.findById("r1")).thenReturn(Optional.of(registro));
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioConSancion(TipoSancion.BAN, null)));

        ResolverApelacionRequestDTO request = ResolverApelacionRequestDTO.builder()
                .aprobar(false).respuesta("Apelación rechazada").build();
        servicio.resolverApelacion("r1", request, "admin1");

        assertThat(registro.getEstadoApelacion()).isEqualTo(EstadoApelacion.RECHAZADA);
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(notificacionService).notificarSistema(eq(NOMBRE), eq("Apelación rechazada"), anyString());
    }

    @Test
    void resolverApelacion_sinApelacionPendiente_lanzaOperacionInvalida() {
        RegistroModeracion registro = RegistroModeracion.builder()
                .id("r1")
                .usuarioId(USUARIO_ID.toHexString())
                .accion(AccionModeracion.DETECCION)
                .build();
        when(registroModeracionRepository.findById("r1")).thenReturn(Optional.of(registro));

        ResolverApelacionRequestDTO request = ResolverApelacionRequestDTO.builder()
                .aprobar(true).respuesta("Respuesta").build();
        assertThatThrownBy(() -> servicio.resolverApelacion("r1", request, "admin1"))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("apelación pendiente");
    }
}