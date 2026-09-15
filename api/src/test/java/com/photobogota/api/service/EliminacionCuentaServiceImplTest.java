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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.photobogota.api.dto.ConfirmarEliminacionDTO;
import com.photobogota.api.dto.EstadoEliminacionDTO;
import com.photobogota.api.dto.SolicitarEliminacionDTO;
import com.photobogota.api.exception.AccessForbiddenException;
import com.photobogota.api.exception.InvalidCredentialsException;
import com.photobogota.api.exception.OperacionInvalidaException;
import com.photobogota.api.model.CodigoEliminacionCuenta;
import com.photobogota.api.model.EstadoSolicitudEliminacion;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.SolicitudEliminacionCuenta;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.CodigoEliminacionRepository;
import com.photobogota.api.repository.RefreshTokenRepository;
import com.photobogota.api.repository.SesionRepository;
import com.photobogota.api.repository.SolicitudEliminacionRepository;
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
class EliminacionCuentaServiceImplTest {

    @Mock
    private UsuarioAuthRepository usuarioAuthRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SolicitudEliminacionRepository solicitudEliminacionRepository;

    @Mock
    private CodigoEliminacionRepository codigoEliminacionRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private SesionRepository sesionRepository;

    @Mock
    private IEmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EliminacionCuentaServiceImpl servicio;

    private static final ObjectId USUARIO_ID = new ObjectId();
    private static final String EMAIL = "juan@mail.com";
    private static final String NOMBRE = "juan";

    private UsuarioAuth authMiembro() {
        return UsuarioAuth.builder()
                .id(USUARIO_ID)
                .email(EMAIL)
                .nombreUsuario(NOMBRE)
                .contrasena("pass")
                .rol(Rol.MIEMBRO)
                .build();
    }

    private Miembro perfilActivo() {
        return Miembro.builder()
                .id(USUARIO_ID)
                .nombresCompletos("Juan Pérez")
                .estadoCuenta(true)
                .build();
    }

    private SolicitudEliminacionCuenta solicitud(EstadoSolicitudEliminacion estado) {
        return SolicitudEliminacionCuenta.builder()
                .id(new ObjectId())
                .usuarioId(USUARIO_ID)
                .email(EMAIL)
                .nombreUsuario(NOMBRE)
                .motivo(com.photobogota.api.model.MotivoEliminacionCuenta.PREOCUPACIONES_DE_PRIVACIDAD)
                .comentario("Motivo de prueba")
                .estado(estado)
                .build();
    }

    @Test
    void solicitarEliminacion_miembroValido_generaCodigoYEnviaCorreo() {
        when(usuarioAuthRepository.findByNombreUsuario(NOMBRE)).thenReturn(Optional.of(authMiembro()));
        when(solicitudEliminacionRepository.findByUsuarioIdAndEstadoIn(eq(USUARIO_ID), anyList()))
                .thenReturn(Optional.empty());
        when(emailService.construirHtmlSolicitudEliminacion(eq(NOMBRE), anyString())).thenReturn("html");

        SolicitarEliminacionDTO dto = SolicitarEliminacionDTO.builder()
                .motivo(com.photobogota.api.model.MotivoEliminacionCuenta.PREOCUPACIONES_DE_PRIVACIDAD)
                .comentario("Motivo de prueba")
                .build();

        String resultado = servicio.solicitarEliminacion(NOMBRE, dto);

        assertThat(resultado).contains("código de verificación");
        verify(codigoEliminacionRepository).deleteByEmail(EMAIL);
        verify(emailService).enviarCorreoHtml(EMAIL, "Eliminar cuenta - PhotoBogota", "html");

        ArgumentCaptor<CodigoEliminacionCuenta> captorCodigo = ArgumentCaptor.forClass(CodigoEliminacionCuenta.class);
        verify(codigoEliminacionRepository).save(captorCodigo.capture());
        CodigoEliminacionCuenta codigo = captorCodigo.getValue();
        assertThat(codigo.getEmail()).isEqualTo(EMAIL);
        assertThat(codigo.getCodigo()).matches("\\d{6}");
        assertThat(codigo.isUsado()).isFalse();
        assertThat(codigo.getFechaExpiracion()).isAfter(codigo.getFechaCreacion());

        ArgumentCaptor<SolicitudEliminacionCuenta> captorSolicitud =
                ArgumentCaptor.forClass(SolicitudEliminacionCuenta.class);
        verify(solicitudEliminacionRepository).save(captorSolicitud.capture());
        SolicitudEliminacionCuenta solicitud = captorSolicitud.getValue();
        assertThat(solicitud.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(solicitud.getEmail()).isEqualTo(EMAIL);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudEliminacion.PENDIENTE_VERIFICACION);
        assertThat(solicitud.getMotivo()).isEqualTo(com.photobogota.api.model.MotivoEliminacionCuenta
                .PREOCUPACIONES_DE_PRIVACIDAD);
        assertThat(solicitud.getComentario()).isEqualTo("Motivo de prueba");
    }

    @Test
    void solicitarEliminacion_usuarioNoEncontrado_lanzaInvalidCredentials() {
        when(usuarioAuthRepository.findByNombreUsuario(NOMBRE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.solicitarEliminacion(NOMBRE,
                SolicitarEliminacionDTO.builder().build()))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void solicitarEliminacion_noMiembro_lanzaAccessForbidden() {
        when(usuarioAuthRepository.findByNombreUsuario(NOMBRE)).thenReturn(Optional.of(
                UsuarioAuth.builder().id(USUARIO_ID).rol(Rol.MOD).build()));

        assertThatThrownBy(() -> servicio.solicitarEliminacion(NOMBRE,
                SolicitarEliminacionDTO.builder().build()))
                .isInstanceOf(AccessForbiddenException.class)
                .hasMessageContaining("MIEMBRO");
    }

    @Test
    void solicitarEliminacion_conSolicitudEnCurso_lanzaOperacionInvalida() {
        when(usuarioAuthRepository.findByNombreUsuario(NOMBRE)).thenReturn(Optional.of(authMiembro()));
        when(solicitudEliminacionRepository.findByUsuarioIdAndEstadoIn(eq(USUARIO_ID), anyList()))
                .thenReturn(Optional.of(solicitud(EstadoSolicitudEliminacion.PENDIENTE_VERIFICACION)));

        assertThatThrownBy(() -> servicio.solicitarEliminacion(NOMBRE,
                SolicitarEliminacionDTO.builder().build()))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("en curso");
    }

    @Test
    void confirmarEliminacion_codigoValido_desactivaCuentaYPrograma() {
        when(usuarioAuthRepository.findByNombreUsuario(NOMBRE)).thenReturn(Optional.of(authMiembro()));
        CodigoEliminacionCuenta codigo = CodigoEliminacionCuenta.builder()
                .email(EMAIL)
                .codigo("123456")
                .fechaCreacion(LocalDateTime.now().minusMinutes(1))
                .fechaExpiracion(LocalDateTime.now().plusMinutes(15))
                .usado(false)
                .build();
        when(codigoEliminacionRepository.findByEmailAndCodigo(EMAIL, "123456"))
                .thenReturn(Optional.of(codigo));
        when(solicitudEliminacionRepository.findByUsuarioIdAndEstadoIn(
                eq(USUARIO_ID), anyList()))
                .thenReturn(Optional.of(solicitud(EstadoSolicitudEliminacion.PENDIENTE_VERIFICACION)));
        Miembro perfil = perfilActivo();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(perfil));
        when(emailService.construirHtmlConfirmacionEliminacion(eq(NOMBRE), anyString())).thenReturn("html");

        ConfirmarEliminacionDTO dto = ConfirmarEliminacionDTO.builder().codigo("123456").build();
        String resultado = servicio.confirmarEliminacion(NOMBRE, dto);

        assertThat(resultado).contains("desactivada");
        assertThat(codigo.isUsado()).isTrue();
        verify(codigoEliminacionRepository).save(codigo);
        verify(refreshTokenRepository).deleteByEmail(EMAIL);
        verify(sesionRepository).deleteByUsuarioId(USUARIO_ID.toString());
        verify(emailService).enviarCorreoHtml(EMAIL, "Eliminación de cuenta confirmada - PhotoBogota", "html");

        ArgumentCaptor<SolicitudEliminacionCuenta> captor =
                ArgumentCaptor.forClass(SolicitudEliminacionCuenta.class);
        verify(solicitudEliminacionRepository).save(captor.capture());
        SolicitudEliminacionCuenta guardada = captor.getValue();
        assertThat(guardada.getEstado()).isEqualTo(EstadoSolicitudEliminacion.PROGRAMADA);
        assertThat(guardada.getFechaConfirmacion()).isNotNull();
        assertThat(guardada.getFechaProgramadaEliminacion()).isNotNull();

        assertThat(perfil.getEstadoCuenta()).isFalse();
    }

    @Test
    void confirmarEliminacion_codigoInvalido_lanzaInvalidCredentials() {
        when(usuarioAuthRepository.findByNombreUsuario(NOMBRE)).thenReturn(Optional.of(authMiembro()));
        when(codigoEliminacionRepository.findByEmailAndCodigo(EMAIL, "000000"))
                .thenReturn(Optional.empty());

        ConfirmarEliminacionDTO dto = ConfirmarEliminacionDTO.builder().codigo("000000").build();
        assertThatThrownBy(() -> servicio.confirmarEliminacion(NOMBRE, dto))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Código de verificación inválido");
    }

    @Test
    void confirmarEliminacion_codigoYaUsado_lanzaInvalidCredentials() {
        when(usuarioAuthRepository.findByNombreUsuario(NOMBRE)).thenReturn(Optional.of(authMiembro()));
        CodigoEliminacionCuenta codigo = CodigoEliminacionCuenta.builder()
                .email(EMAIL)
                .codigo("123456")
                .fechaExpiracion(LocalDateTime.now().plusMinutes(15))
                .usado(true)
                .build();
        when(codigoEliminacionRepository.findByEmailAndCodigo(EMAIL, "123456"))
                .thenReturn(Optional.of(codigo));

        ConfirmarEliminacionDTO dto = ConfirmarEliminacionDTO.builder().codigo("123456").build();
        assertThatThrownBy(() -> servicio.confirmarEliminacion(NOMBRE, dto))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("ya ha sido utilizado");
    }

    @Test
    void confirmarEliminacion_codigoExpirado_lanzaInvalidCredentials() {
        when(usuarioAuthRepository.findByNombreUsuario(NOMBRE)).thenReturn(Optional.of(authMiembro()));
        CodigoEliminacionCuenta codigo = CodigoEliminacionCuenta.builder()
                .email(EMAIL)
                .codigo("123456")
                .fechaExpiracion(LocalDateTime.now().minusMinutes(1))
                .usado(false)
                .build();
        when(codigoEliminacionRepository.findByEmailAndCodigo(EMAIL, "123456"))
                .thenReturn(Optional.of(codigo));

        ConfirmarEliminacionDTO dto = ConfirmarEliminacionDTO.builder().codigo("123456").build();
        assertThatThrownBy(() -> servicio.confirmarEliminacion(NOMBRE, dto))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("expirado");
    }

    @Test
    void confirmarEliminacion_sinSolicitudPendiente_lanzaOperacionInvalida() {
        when(usuarioAuthRepository.findByNombreUsuario(NOMBRE)).thenReturn(Optional.of(authMiembro()));
        CodigoEliminacionCuenta codigo = CodigoEliminacionCuenta.builder()
                .email(EMAIL)
                .codigo("123456")
                .fechaExpiracion(LocalDateTime.now().plusMinutes(15))
                .usado(false)
                .build();
        when(codigoEliminacionRepository.findByEmailAndCodigo(EMAIL, "123456"))
                .thenReturn(Optional.of(codigo));
        when(solicitudEliminacionRepository.findByUsuarioIdAndEstadoIn(eq(USUARIO_ID), anyList()))
                .thenReturn(Optional.empty());

        ConfirmarEliminacionDTO dto = ConfirmarEliminacionDTO.builder().codigo("123456").build();
        assertThatThrownBy(() -> servicio.confirmarEliminacion(NOMBRE, dto))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("ninguna solicitud de eliminación pendiente");
    }

    @Test
    void cancelarEliminacion_dentroDelPlazo_reactivaCuenta() {
        when(usuarioAuthRepository.findByNombreUsuario(NOMBRE)).thenReturn(Optional.of(authMiembro()));
        SolicitudEliminacionCuenta solicitud =
                solicitud(EstadoSolicitudEliminacion.PROGRAMADA);
        solicitud.setFechaProgramadaEliminacion(LocalDateTime.now().plusDays(10));
        when(solicitudEliminacionRepository.findByUsuarioIdAndEstadoIn(eq(USUARIO_ID), anyList()))
                .thenReturn(Optional.of(solicitud));
        Miembro perfil = perfilActivo();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(perfil));
        when(emailService.construirHtmlCancelacionEliminacion(NOMBRE)).thenReturn("html");

        String resultado = servicio.cancelarEliminacion(NOMBRE);

        assertThat(resultado).contains("activa nuevamente");
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudEliminacion.CANCELADA);
        assertThat(solicitud.getFechaCancelacion()).isNotNull();
        verify(solicitudEliminacionRepository).save(solicitud);
        verify(usuarioRepository).save(perfil);
        assertThat(perfil.getEstadoCuenta()).isTrue();
        verify(codigoEliminacionRepository).deleteByEmail(EMAIL);
        verify(emailService).enviarCorreoHtml(EMAIL, "Tu cuenta fue reactivada - PhotoBogota", "html");
    }

    @Test
    void cancelarEliminacion_plazoVencido_lanzaOperacionInvalida() {
        when(usuarioAuthRepository.findByNombreUsuario(NOMBRE)).thenReturn(Optional.of(authMiembro()));
        SolicitudEliminacionCuenta solicitud =
                solicitud(EstadoSolicitudEliminacion.PROGRAMADA);
        solicitud.setFechaProgramadaEliminacion(LocalDateTime.now().minusDays(1));
        when(solicitudEliminacionRepository.findByUsuarioIdAndEstadoIn(eq(USUARIO_ID), anyList()))
                .thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> servicio.cancelarEliminacion(NOMBRE))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("30 días");
    }

    @Test
    void cancelarEliminacion_sinSolicitudActiva_lanzaOperacionInvalida() {
        when(usuarioAuthRepository.findByNombreUsuario(NOMBRE)).thenReturn(Optional.of(authMiembro()));
        when(solicitudEliminacionRepository.findByUsuarioIdAndEstadoIn(eq(USUARIO_ID), anyList()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.cancelarEliminacion(NOMBRE))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("ninguna solicitud de eliminación activa");
    }

    @Test
    void obtenerEstado_conSolicitudProgramada_calculaDiasRestantes() {
        when(usuarioAuthRepository.findByNombreUsuario(NOMBRE)).thenReturn(Optional.of(authMiembro()));
        SolicitudEliminacionCuenta solicitud =
                solicitud(EstadoSolicitudEliminacion.PROGRAMADA);
        solicitud.setFechaProgramadaEliminacion(LocalDateTime.now().plusDays(10).plusSeconds(60));
        when(solicitudEliminacionRepository.findByUsuarioIdAndEstadoIn(eq(USUARIO_ID), anyList()))
                .thenReturn(Optional.of(solicitud));

        EstadoEliminacionDTO dto = servicio.obtenerEstado(NOMBRE);

        assertThat(dto.isTieneSolicitudActiva()).isTrue();
        assertThat(dto.getEstado()).isEqualTo("PROGRAMADA");
        assertThat(dto.getDiasRestantes()).isEqualTo(10L);
        assertThat(dto.getMotivo()).isEqualTo(com.photobogota.api.model.MotivoEliminacionCuenta
                .PREOCUPACIONES_DE_PRIVACIDAD);
    }

    @Test
    void obtenerEstado_sinSolicitud_devuelveInactiva() {
        when(usuarioAuthRepository.findByNombreUsuario(NOMBRE)).thenReturn(Optional.of(authMiembro()));
        when(solicitudEliminacionRepository.findByUsuarioIdAndEstadoIn(eq(USUARIO_ID), anyList()))
                .thenReturn(Optional.empty());

        EstadoEliminacionDTO dto = servicio.obtenerEstado(NOMBRE);

        assertThat(dto.isTieneSolicitudActiva()).isFalse();
    }

    @Test
    void procesarEliminacionesVencidas_sinSolicitudes_noHaceNada() {
        when(solicitudEliminacionRepository.findByEstadoAndFechaProgramadaEliminacionBefore(
                eq(EstadoSolicitudEliminacion.PROGRAMADA), any(LocalDateTime.class)))
                .thenReturn(List.of());

        servicio.procesarEliminacionesVencidas();

        verify(solicitudEliminacionRepository, never()).save(any(SolicitudEliminacionCuenta.class));
    }

    @Test
    void procesarEliminacionesVencidas_conSolicitudValida_anonimizaCuenta() {
        SolicitudEliminacionCuenta solicitud = solicitud(EstadoSolicitudEliminacion.PROGRAMADA);
        when(solicitudEliminacionRepository.findByEstadoAndFechaProgramadaEliminacionBefore(
                eq(EstadoSolicitudEliminacion.PROGRAMADA), any(LocalDateTime.class)))
                .thenReturn(List.of(solicitud));
        UsuarioAuth auth = authMiembro();
        Miembro perfil = perfilActivo();
        when(usuarioAuthRepository.findById(USUARIO_ID)).thenReturn(Optional.of(auth));
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(perfil));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(emailService.construirHtmlEliminacionCompletada(NOMBRE)).thenReturn("html");

        servicio.procesarEliminacionesVencidas();

        assertThat(perfil.getNombresCompletos()).isEqualTo("Usuario eliminado");
        assertThat(perfil.getEstadoCuenta()).isFalse();
        assertThat(auth.getEmail()).endsWith("@anonimizado.photobogota.com");
        assertThat(auth.getNombreUsuario()).startsWith("usuario_eliminado_");
        assertThat(auth.getContrasena()).isEqualTo("hash");
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudEliminacion.COMPLETADA);
        verify(refreshTokenRepository).deleteByEmail(EMAIL);
        verify(sesionRepository).deleteByUsuarioId(USUARIO_ID.toString());
        verify(emailService).enviarCorreoHtml(EMAIL, "Tu cuenta fue eliminada - PhotoBogota", "html");
        verify(solicitudEliminacionRepository).save(solicitud);
    }

    @Test
    void procesarEliminacionesVencidas_sinUsuario_marcaCompletada() {
        SolicitudEliminacionCuenta solicitud = solicitud(EstadoSolicitudEliminacion.PROGRAMADA);
        when(solicitudEliminacionRepository.findByEstadoAndFechaProgramadaEliminacionBefore(
                eq(EstadoSolicitudEliminacion.PROGRAMADA), any(LocalDateTime.class)))
                .thenReturn(List.of(solicitud));

        servicio.procesarEliminacionesVencidas();

        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudEliminacion.COMPLETADA);
        assertThat(solicitud.getFechaCompletada()).isNotNull();
        verify(solicitudEliminacionRepository).save(solicitud);
    }
}