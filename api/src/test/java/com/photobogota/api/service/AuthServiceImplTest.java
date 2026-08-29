package com.photobogota.api.service;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.photobogota.api.config.JwtService;
import com.photobogota.api.dto.LoginRequestDTO;
import com.photobogota.api.dto.LoginResponseDTO;
import com.photobogota.api.dto.LogoutResponseDTO;
import com.photobogota.api.dto.RegistroRequestDTO;
import com.photobogota.api.dto.RegistroResponseDTO;
import com.photobogota.api.dto.SolicitarRecuperacionDTO;
import com.photobogota.api.dto.UsuarioResumenDTO;
import com.photobogota.api.dto.VerificarCodigoDTO;
import com.photobogota.api.exception.EmailAlreadyExistsException;
import com.photobogota.api.exception.InvalidCredentialsException;
import com.photobogota.api.exception.RegistroException;
import com.photobogota.api.exception.UsernameAlreadyExistsException;
import com.photobogota.api.mapper.UsuarioMapper;
import com.photobogota.api.model.CodigoRecuperacion;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.Sesion;
import com.photobogota.api.model.Usuario;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.CodigoRecuperacionRepository;
import com.photobogota.api.repository.SesionRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioAuthRepository usuarioAuthRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioMapper usuarioMapper;

    @Mock
    private JwtService jwtService;

    @Mock
    private IRefreshToken refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CodigoRecuperacionRepository codigoRecuperacionRepository;

    @Mock
    private IEmailService emailService;

    @Mock
    private SesionRepository sesionRepository;

    @Mock
    private IFiltroContenidoService filtroContenidoService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Miembro miembroDeEjemplo(ObjectId id) {
        return Miembro.builder()
                .id(id)
                .nombresCompletos("Juan Pérez")
                .nivel(3)
                .puntos(100L)
                .estadoCuenta(true)
                .build();
    }

    private UsuarioAuth authDeEjemplo(ObjectId id) {
        return UsuarioAuth.builder()
                .id(id)
                .nombreUsuario("juanperez")
                .email("juan@photobogota.com")
                .rol(Rol.MIEMBRO)
                .contrasena("hash")
                .build();
    }

    private RegistroRequestDTO registroDeEjemplo() {
        RegistroRequestDTO dto = new RegistroRequestDTO();
        dto.setNombresCompletos("Juan Pérez");
        dto.setEmail("juan@photobogota.com");
        dto.setNombreUsuario("juanperez");
        dto.setContrasena("Segura123.");
        return dto;
    }

    @Test
    void registrar_datosValidos_registraEnAmbasColecciones() {
        when(usuarioAuthRepository.existsByEmail("juan@photobogota.com")).thenReturn(false);
        when(usuarioAuthRepository.existsByNombreUsuario("juanperez")).thenReturn(false);
        when(usuarioMapper.toMiembroEntity(any(RegistroRequestDTO.class))).thenReturn(new Miembro());

        RegistroResponseDTO respuesta = authService.registrar(registroDeEjemplo());

        assertThat(respuesta.getMensaje()).isEqualTo("Usuario registrado exitosamente");
        verify(usuarioRepository).save(any(Usuario.class));
        verify(usuarioAuthRepository).save(any(UsuarioAuth.class));
    }

    @Test
    void registrar_emailYaRegistrado_lanzaEmailAlreadyExists() {
        when(usuarioAuthRepository.existsByEmail("juan@photobogota.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(registroDeEjemplo()))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrar_nombreDeUsuarioYaEnUso_lanzaUsernameAlreadyExists() {
        when(usuarioAuthRepository.existsByEmail("juan@photobogota.com")).thenReturn(false);
        when(usuarioAuthRepository.existsByNombreUsuario("juanperez")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(registroDeEjemplo()))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void registrar_errorAlPersistir_lanzaRegistroException() {
        when(usuarioAuthRepository.existsByEmail("juan@photobogota.com")).thenReturn(false);
        when(usuarioAuthRepository.existsByNombreUsuario("juanperez")).thenReturn(false);
        when(usuarioMapper.toMiembroEntity(any(RegistroRequestDTO.class))).thenReturn(new Miembro());
        when(usuarioRepository.save(any(Usuario.class))).thenThrow(new RuntimeException("fallo de base de datos"));

        assertThatThrownBy(() -> authService.registrar(registroDeEjemplo()))
                .isInstanceOf(RegistroException.class);
    }

    @Test
    void login_credencialesValidas_devuelveTokensYDatosDelPerfil() {
        ObjectId id = new ObjectId();
        when(usuarioAuthRepository.findByEmailOrNombreUsuario(eq("juanperez"), eq("juanperez")))
                .thenReturn(java.util.Optional.of(authDeEjemplo(id)));
        when(passwordEncoder.matches("Clave123.", "hash")).thenReturn(true);
        when(usuarioRepository.findById(id)).thenReturn(java.util.Optional.of(miembroDeEjemplo(id)));
        when(jwtService.generarToken(anyMap(), eq("juanperez"))).thenReturn("access-token");
        when(refreshTokenService.crearRefreshToken("juan@photobogota.com")).thenReturn("refresh-token");

        LoginRequestDTO request = new LoginRequestDTO();
        request.setLogin("juanperez");
        request.setContrasena("Clave123.");

        LoginResponseDTO respuesta = authService.login(request);

        assertThat(respuesta.getToken()).isEqualTo("access-token");
        assertThat(respuesta.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(respuesta.getNivel()).isEqualTo(3);
        assertThat(respuesta.getPuntos()).isEqualTo(100L);
        assertThat(respuesta.getNombresCompletos()).isEqualTo("Juan Pérez");
        assertThat(respuesta.getEstadoCuenta()).isTrue();
        verify(sesionRepository).deleteByUsuarioId(id.toString());
        verify(sesionRepository).save(any(Sesion.class));
    }

    @Test
    void login_usuarioNoEncontrado_lanzaInvalidCredentials() {
        when(usuarioAuthRepository.findByEmailOrNombreUsuario(eq("ghost"), eq("ghost")))
                .thenReturn(java.util.Optional.empty());

        LoginRequestDTO request = new LoginRequestDTO();
        request.setLogin("ghost");
        request.setContrasena("Cualquiera1.");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_contrasenaIncorrecta_lanzaInvalidCredentials() {
        ObjectId id = new ObjectId();
        when(usuarioAuthRepository.findByEmailOrNombreUsuario(eq("juanperez"), eq("juanperez")))
                .thenReturn(java.util.Optional.of(authDeEjemplo(id)));
        when(passwordEncoder.matches("Mal123.", "hash")).thenReturn(false);

        LoginRequestDTO request = new LoginRequestDTO();
        request.setLogin("juanperez");
        request.setContrasena("Mal123.");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_perfilInexistente_lanzaRuntimeException() {
        ObjectId id = new ObjectId();
        when(usuarioAuthRepository.findByEmailOrNombreUsuario(eq("juanperez"), eq("juanperez")))
                .thenReturn(java.util.Optional.of(authDeEjemplo(id)));
        when(passwordEncoder.matches("Clave123.", "hash")).thenReturn(true);
        when(usuarioRepository.findById(id)).thenReturn(java.util.Optional.empty());

        LoginRequestDTO request = new LoginRequestDTO();
        request.setLogin("juanperez");
        request.setContrasena("Clave123.");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Perfil de usuario no encontrado");
    }

    @Test
    void refreshToken_valido_generaNuevosTokensYActualizaSesion() {
        ObjectId id = new ObjectId();
        Sesion sesion = Sesion.builder()
                .refreshToken("refresh-token")
                .usuarioId(id.toString())
                .nombreUsuario("juanperez")
                .rol("MIEMBRO")
                .activo(true)
                .build();
        when(refreshTokenService.obtenerEmailSiValido("refresh-token")).thenReturn("juan@photobogota.com");
        when(usuarioAuthRepository.findByEmail("juan@photobogota.com")).thenReturn(java.util.Optional.of(authDeEjemplo(id)));
        when(sesionRepository.findByRefreshTokenAndActivoTrue("refresh-token")).thenReturn(java.util.Optional.of(sesion));
        when(usuarioRepository.findById(id)).thenReturn(java.util.Optional.of(miembroDeEjemplo(id)));
        when(jwtService.generarToken(anyMap(), eq("juanperez"))).thenReturn("nuevo-access-token");
        when(refreshTokenService.crearRefreshToken("juan@photobogota.com")).thenReturn("nuevo-refresh-token");

        LoginResponseDTO respuesta = authService.refreshToken("refresh-token");

        assertThat(respuesta.getToken()).isEqualTo("nuevo-access-token");
        assertThat(respuesta.getRefreshToken()).isEqualTo("nuevo-refresh-token");
        assertThat(respuesta.getMensaje()).isEqualTo("Token refrescado exitosamente");
        assertThat(sesion.getRefreshToken()).isEqualTo("nuevo-refresh-token");
        verify(sesionRepository).save(sesion);
    }

    @Test
    void refreshToken_sesionNoActiva_lanzaRuntimeException() {
        ObjectId id = new ObjectId();
        when(refreshTokenService.obtenerEmailSiValido("refresh-token")).thenReturn("juan@photobogota.com");
        when(usuarioAuthRepository.findByEmail("juan@photobogota.com")).thenReturn(java.util.Optional.of(authDeEjemplo(id)));
        when(sesionRepository.findByRefreshTokenAndActivoTrue("refresh-token")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken("refresh-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Sesión inválida o expirada");
    }

    @Test
    void logout_revocaElRefreshTokenYEliminaLaSesion() {
        LogoutResponseDTO respuesta = authService.logout("refresh-token");

        assertThat(respuesta.getMensaje()).isEqualTo("Sesión cerrada exitosamente");
        verify(refreshTokenService).revocarToken("refresh-token");
        verify(sesionRepository).deleteByRefreshToken("refresh-token");
    }

    @Test
    void getResumenUsuario_devuelveDatosDelPerfil() {
        ObjectId id = new ObjectId();
        when(usuarioAuthRepository.findByNombreUsuario("juanperez"))
                .thenReturn(java.util.Optional.of(authDeEjemplo(id)));
        when(usuarioRepository.findById(id)).thenReturn(java.util.Optional.of(miembroDeEjemplo(id)));

        UsuarioResumenDTO resumen = authService.getResumenUsuario("juanperez");

        assertThat(resumen.getNombreUsuario()).isEqualTo("juanperez");
        assertThat(resumen.getEmail()).isEqualTo("juan@photobogota.com");
        assertThat(resumen.getNivel()).isEqualTo(3);
        assertThat(resumen.getPuntos()).isEqualTo(100L);
    }

    @Test
    void getResumenUsuario_inexistente_lanzaInvalidCredentials() {
        when(usuarioAuthRepository.findByNombreUsuario("ghost")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> authService.getResumenUsuario("ghost"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void solicitarRecuperacion_emailNoRegistrado_noRevelaNadaPermiteAvanzar() {
        when(usuarioAuthRepository.findByEmail("ghost@photobogota.com")).thenReturn(java.util.Optional.empty());

        SolicitarRecuperacionDTO dto = SolicitarRecuperacionDTO.builder()
                .email("ghost@photobogota.com")
                .build();

        String mensaje = authService.solicitarRecuperacionContrasena(dto);

        assertThat(mensaje).isEqualTo("Se ha enviado un código de verificación a tu correo electrónico");
        verify(emailService, never()).enviarCorreoHtml(any(), any(), any());
    }

    @Test
    void solicitarRecuperacion_emailRegistrado_guardaCodigoDeSeisDigitosYEnviaCorreo() {
        ObjectId id = new ObjectId();
        when(usuarioAuthRepository.findByEmail("juan@photobogota.com"))
                .thenReturn(java.util.Optional.of(authDeEjemplo(id)));

        SolicitarRecuperacionDTO dto = SolicitarRecuperacionDTO.builder()
                .email("juan@photobogota.com")
                .build();

        String mensaje = authService.solicitarRecuperacionContrasena(dto);

        assertThat(mensaje).isEqualTo("Se ha enviado un código de verificación a tu correo electrónico");
        ArgumentCaptor<CodigoRecuperacion> captor = ArgumentCaptor.forClass(CodigoRecuperacion.class);
        verify(codigoRecuperacionRepository).deleteByEmail("juan@photobogota.com");
        verify(codigoRecuperacionRepository).save(captor.capture());
        assertThat(captor.getValue().getCodigo()).matches("\\d{6}");
        assertThat(captor.getValue().isUsado()).isFalse();
        verify(emailService).enviarCorreoHtml(eq("juan@photobogota.com"), any(), any());
    }

    @Test
    void verificarCodigo_valido_cambiaLaContrasena() {
        CodigoRecuperacion codigo = CodigoRecuperacion.builder()
                .email("juan@photobogota.com")
                .codigo("123456")
                .fechaExpiracion(LocalDateTime.now().plusMinutes(15))
                .usado(false)
                .build();
        ObjectId id = new ObjectId();
        when(codigoRecuperacionRepository.findByEmailAndCodigo("juan@photobogota.com", "123456"))
                .thenReturn(java.util.Optional.of(codigo));
        when(usuarioAuthRepository.findByEmail("juan@photobogota.com"))
                .thenReturn(java.util.Optional.of(authDeEjemplo(id)));
        when(passwordEncoder.encode("NuevaClave123.")).thenReturn("nuevo-hash");

        VerificarCodigoDTO dto = VerificarCodigoDTO.builder()
                .email("juan@photobogota.com")
                .codigo("123456")
                .nuevaContrasena("NuevaClave123.")
                .build();

        String mensaje = authService.verificarCodigoYCambiarContrasena(dto);

        assertThat(mensaje).contains("Contraseña actualizada exitosamente");
        assertThat(codigo.isUsado()).isTrue();
        verify(codigoRecuperacionRepository).save(codigo);
        verify(usuarioAuthRepository).save(any(UsuarioAuth.class));
    }

    @Test
    void verificarCodigo_invalido_lanzaInvalidCredentials() {
        when(codigoRecuperacionRepository.findByEmailAndCodigo("juan@photobogota.com", "000000"))
                .thenReturn(java.util.Optional.empty());

        VerificarCodigoDTO dto = VerificarCodigoDTO.builder()
                .email("juan@photobogota.com")
                .codigo("000000")
                .nuevaContrasena("NuevaClave123.")
                .build();

        assertThatThrownBy(() -> authService.verificarCodigoYCambiarContrasena(dto))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Código de verificación inválido");
    }

    @Test
    void verificarCodigo_yaUsado_lanzaInvalidCredentials() {
        CodigoRecuperacion codigo = CodigoRecuperacion.builder()
                .email("juan@photobogota.com")
                .codigo("123456")
                .fechaExpiracion(LocalDateTime.now().plusMinutes(15))
                .usado(true)
                .build();
        when(codigoRecuperacionRepository.findByEmailAndCodigo("juan@photobogota.com", "123456"))
                .thenReturn(java.util.Optional.of(codigo));

        VerificarCodigoDTO dto = VerificarCodigoDTO.builder()
                .email("juan@photobogota.com")
                .codigo("123456")
                .nuevaContrasena("NuevaClave123.")
                .build();

        assertThatThrownBy(() -> authService.verificarCodigoYCambiarContrasena(dto))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("ya ha sido utilizado");
    }

    @Test
    void verificarCodigo_expirado_lanzaInvalidCredentials() {
        CodigoRecuperacion codigo = CodigoRecuperacion.builder()
                .email("juan@photobogota.com")
                .codigo("123456")
                .fechaExpiracion(LocalDateTime.now().minusMinutes(1))
                .usado(false)
                .build();
        when(codigoRecuperacionRepository.findByEmailAndCodigo("juan@photobogota.com", "123456"))
                .thenReturn(java.util.Optional.of(codigo));

        VerificarCodigoDTO dto = VerificarCodigoDTO.builder()
                .email("juan@photobogota.com")
                .codigo("123456")
                .nuevaContrasena("NuevaClave123.")
                .build();

        assertThatThrownBy(() -> authService.verificarCodigoYCambiarContrasena(dto))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("ha expirado");
    }
}