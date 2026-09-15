package com.photobogota.api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.photobogota.api.dto.AspiranteResponseDTO;
import com.photobogota.api.dto.EstadisticasAspiranteDTO;
import com.photobogota.api.dto.ReenvioDocumentosDTO;
import com.photobogota.api.dto.SolicitudAspiranteDTO;
import com.photobogota.api.exception.AspiranteAlreadyExistsException;
import com.photobogota.api.exception.OperacionInvalidaException;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.mapper.AspiranteMapper;
import com.photobogota.api.model.Aspirante;
import com.photobogota.api.model.EstadoAspirante;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.Socio;
import com.photobogota.api.model.Usuario;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.AspiranteRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;

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
class AspiranteServiceImplTest {

    @Mock
    private AspiranteRepository aspiranteRepository;

    @Mock
    private AspiranteMapper aspiranteMapper;

    @Mock
    private IEmailService emailService;

    @Mock
    private INotificacionService notificacionService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioAuthRepository usuarioAuthRepository;

    @Mock
    private UsuarioFactory usuarioFactory;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AspiranteServiceImpl aspiranteService;

    @BeforeEach
    void configurarValoresDeConfiguracion() {
        ReflectionTestUtils.setField(aspiranteService, "frontendUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(aspiranteService, "soporteEmail", "photobogota123@gmail.com");
        ReflectionTestUtils.setField(aspiranteService, "manualSocioUrl", "https://photobogota.com/manual-socio");
    }

    private Aspirante aspiranteDeEjemplo(EstadoAspirante estado) {
        return Aspirante.builder()
                .id("asp-1")
                .nombres("Juan Sebastian")
                .apellidos("Romero Ramirez")
                .email("juan.romero@example.com")
                .nit("123456789")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .estado(estado)
                .fechaSolicitud(LocalDate.now())
                .codigo("SOL-123456")
                .vecesCorregida(0)
                .build();
    }

    private SolicitudAspiranteDTO solicitudDeEjemplo() {
        return SolicitudAspiranteDTO.builder()
                .nombres("Juan Sebastian")
                .apellidos("Romero Ramirez")
                .email("juan.romero@example.com")
                .nit("123456789")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .rutaArchivo("/uploads/cedula.pdf")
                .build();
    }

    private AspiranteResponseDTO dtoDeEjemplo() {
        return AspiranteResponseDTO.builder()
                .id("asp-1")
                .nombres("Juan Sebastian")
                .estado(EstadoAspirante.PENDIENTE)
                .build();
    }

    @Test
    void crearSolicitud_sinPrevias_guardaConCodigoYEnviaCorreo() {
        when(aspiranteRepository.findByEmailOrderByFechaSolicitudDesc("juan.romero@example.com")).thenReturn(List.of());
        when(aspiranteRepository.findByNitOrderByFechaSolicitudDesc("123456789")).thenReturn(List.of());
        when(aspiranteRepository.save(any(Aspirante.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aspiranteMapper.toResponse(any(Aspirante.class))).thenReturn(dtoDeEjemplo());

        AspiranteResponseDTO resultado = aspiranteService.crearSolicitud(solicitudDeEjemplo());

        assertThat(resultado.getId()).isEqualTo("asp-1");
        ArgumentCaptor<Aspirante> captor = ArgumentCaptor.forClass(Aspirante.class);
        verify(aspiranteRepository).save(captor.capture());
        assertThat(captor.getValue().getCodigo()).matches("SOL-\\d{6}");
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoAspirante.PENDIENTE);
        assertThat(captor.getValue().getFechaSolicitud()).isEqualTo(LocalDate.now());
        verify(emailService).enviarCorreoHtml(eq("juan.romero@example.com"),
                eq("Tu código de solicitud - PhotoBogota"), anyString());
    }

    @Test
    void crearSolicitud_conSolicitudEnTramite_lanzaAspiranteAlreadyExists() {
        when(aspiranteRepository.findByEmailOrderByFechaSolicitudDesc("juan.romero@example.com"))
                .thenReturn(List.of(aspiranteDeEjemplo(EstadoAspirante.PENDIENTE)));
        when(aspiranteRepository.findByNitOrderByFechaSolicitudDesc("123456789")).thenReturn(List.of());

        assertThatThrownBy(() -> aspiranteService.crearSolicitud(solicitudDeEjemplo()))
                .isInstanceOf(AspiranteAlreadyExistsException.class);
        verify(aspiranteRepository, never()).save(any());
    }

    @Test
    void crearSolicitud_rechazadoReciente_bloqueaPor90Dias() {
        Aspirante rechazado = aspiranteDeEjemplo(EstadoAspirante.RECHAZADO);
        rechazado.setFechaDecision(LocalDateTime.now().minusDays(10));
        when(aspiranteRepository.findByEmailOrderByFechaSolicitudDesc("juan.romero@example.com"))
                .thenReturn(List.of(rechazado));
        when(aspiranteRepository.findByNitOrderByFechaSolicitudDesc("123456789")).thenReturn(List.of());

        assertThatThrownBy(() -> aspiranteService.crearSolicitud(solicitudDeEjemplo()))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("Podrás volver a aplicar");
        verify(aspiranteRepository, never()).save(any());
    }

    @Test
    void crearSolicitud_rechazadoHaceMasDe90Dias_permiteReaplicar() {
        Aspirante rechazado = aspiranteDeEjemplo(EstadoAspirante.RECHAZADO);
        rechazado.setFechaDecision(LocalDateTime.now().minusDays(95));
        when(aspiranteRepository.findByEmailOrderByFechaSolicitudDesc("juan.romero@example.com"))
                .thenReturn(List.of(rechazado));
        when(aspiranteRepository.findByNitOrderByFechaSolicitudDesc("123456789"))
                .thenReturn(List.of(rechazado));
        when(aspiranteRepository.save(any(Aspirante.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aspiranteMapper.toResponse(any(Aspirante.class))).thenReturn(dtoDeEjemplo());

        AspiranteResponseDTO resultado = aspiranteService.crearSolicitud(solicitudDeEjemplo());

        assertThat(resultado.getId()).isEqualTo("asp-1");
        verify(aspiranteRepository).save(any(Aspirante.class));
    }

    @Test
    void obtenerPorId_existente_devuelveDto() {
        when(aspiranteRepository.findById("asp-1")).thenReturn(Optional.of(aspiranteDeEjemplo(EstadoAspirante.PENDIENTE)));
        when(aspiranteMapper.toResponse(any(Aspirante.class))).thenReturn(dtoDeEjemplo());

        AspiranteResponseDTO resultado = aspiranteService.obtenerPorId("asp-1");

        assertThat(resultado.getNombres()).isEqualTo("Juan Sebastian");
    }

    @Test
    void obtenerPorId_inexistente_lanzaResourceNotFound() {
        when(aspiranteRepository.findById("asp-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aspiranteService.obtenerPorId("asp-x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerPorEmail_soloDevuelveLaSolicitudMasReciente() {
        Aspirante antigua = aspiranteDeEjemplo(EstadoAspirante.RECHAZADO);
        antigua.setFechaSolicitud(LocalDate.now().minusYears(1));
        Aspirante reciente = aspiranteDeEjemplo(EstadoAspirante.PENDIENTE);
        when(aspiranteRepository.findByEmailOrderByFechaSolicitudDesc("juan.romero@example.com"))
                .thenReturn(List.of(reciente, antigua));
        when(aspiranteMapper.toResponse(any(Aspirante.class))).thenReturn(dtoDeEjemplo());

        AspiranteResponseDTO resultado = aspiranteService.obtenerPorEmail("juan.romero@example.com");

        assertThat(resultado.getEstado()).isEqualTo(EstadoAspirante.PENDIENTE);
    }

    @Test
    void obtenerPorEmail_inexistente_lanzaResourceNotFound() {
        when(aspiranteRepository.findByEmailOrderByFechaSolicitudDesc("ghost@example.com")).thenReturn(List.of());

        assertThatThrownBy(() -> aspiranteService.obtenerPorEmail("ghost@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerPorCodigo_existente_devuelveDto() {
        when(aspiranteRepository.findByCodigo("SOL-123456"))
                .thenReturn(Optional.of(aspiranteDeEjemplo(EstadoAspirante.PENDIENTE)));
        when(aspiranteMapper.toResponse(any(Aspirante.class))).thenReturn(dtoDeEjemplo());

        AspiranteResponseDTO resultado = aspiranteService.obtenerPorCodigo("SOL-123456");

        assertThat(resultado.getCodigo()).isEqualTo(null);
    }

    @Test
    void obtenerPorCodigo_inexistente_lanzaResourceNotFound() {
        when(aspiranteRepository.findByCodigo("SOL-999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aspiranteService.obtenerPorCodigo("SOL-999999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerTodos_devuelveLaListaCompleta() {
        when(aspiranteRepository.findAll()).thenReturn(List.of(aspiranteDeEjemplo(EstadoAspirante.PENDIENTE)));
        when(aspiranteMapper.toResponseList(any())).thenReturn(List.of(dtoDeEjemplo()));

        List<AspiranteResponseDTO> resultado = aspiranteService.obtenerTodos();

        assertThat(resultado).hasSize(1);
    }

    @Test
    void obtenerPorEstado_filtraPorEstado() {
        when(aspiranteRepository.findByEstado(EstadoAspirante.EN_CORRECCION))
                .thenReturn(List.of(aspiranteDeEjemplo(EstadoAspirante.EN_CORRECCION)));
        when(aspiranteMapper.toResponseList(any())).thenReturn(List.of(dtoDeEjemplo()));

        List<AspiranteResponseDTO> resultado = aspiranteService.obtenerPorEstado(EstadoAspirante.EN_CORRECCION);

        assertThat(resultado).hasSize(1);
        verify(aspiranteRepository).findByEstado(EstadoAspirante.EN_CORRECCION);
    }

    @Test
    void aprobarAspirante_pendiente_pasaAEnvioCredenciales() {
        Aspirante aspirante = aspiranteDeEjemplo(EstadoAspirante.PENDIENTE);
        when(aspiranteRepository.findById("asp-1")).thenReturn(Optional.of(aspirante));
        when(aspiranteRepository.save(any(Aspirante.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aspiranteMapper.toResponse(any(Aspirante.class))).thenReturn(dtoDeEjemplo());

        AspiranteResponseDTO resultado = aspiranteService.aprobarAspirante("asp-1", "mod1");

        assertThat(resultado.getEstado()).isEqualTo(EstadoAspirante.PENDIENTE);
        assertThat(aspirante.getEstado()).isEqualTo(EstadoAspirante.ENVIO_CREDENCIALES);
        assertThat(aspirante.getDecididoPor()).isEqualTo("mod1");
        verify(emailService).enviarCorreoHtml(eq("juan.romero@example.com"),
                eq("Tu solicitud fue aprobada - PhotoBogota"), anyString());
    }

    @Test
    void aprobarAspirante_yaDecidido_lanzaOperacionInvalida() {
        Aspirante aspirante = aspiranteDeEjemplo(EstadoAspirante.APROBADO);
        when(aspiranteRepository.findById("asp-1")).thenReturn(Optional.of(aspirante));

        assertThatThrownBy(() -> aspiranteService.aprobarAspirante("asp-1", "mod1"))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("No se puede procesar");
    }

    @Test
    void rechazarAspirante_pendiente_guardaMotivoYEnviaCorreoDeRechazo() {
        Aspirante aspirante = aspiranteDeEjemplo(EstadoAspirante.PENDIENTE);
        when(aspiranteRepository.findById("asp-1")).thenReturn(Optional.of(aspirante));
        when(aspiranteRepository.save(any(Aspirante.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aspiranteMapper.toResponse(any(Aspirante.class))).thenReturn(dtoDeEjemplo());

        AspiranteResponseDTO resultado = aspiranteService.rechazarAspirante("asp-1", "Documento vencido", "mod1");

        assertThat(aspirante.getEstado()).isEqualTo(EstadoAspirante.RECHAZADO);
        assertThat(aspirante.getMotivoDecision()).isEqualTo("Documento vencido");
        assertThat(aspirante.getFechaDecision()).isNotNull();
        verify(emailService).enviarCorreoHtml(eq("juan.romero@example.com"),
                eq("Tu solicitud fue rechazada - PhotoBogota"), anyString());
    }

    @Test
    void solicitarCorreccion_pendiente_pasaAEnCorreccion() {
        Aspirante aspirante = aspiranteDeEjemplo(EstadoAspirante.PENDIENTE);
        when(aspiranteRepository.findById("asp-1")).thenReturn(Optional.of(aspirante));
        when(aspiranteRepository.save(any(Aspirante.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aspiranteMapper.toResponse(any(Aspirante.class))).thenReturn(dtoDeEjemplo());

        AspiranteResponseDTO resultado = aspiranteService.solicitarCorreccion("asp-1", "Nombrar la razón social", "mod1");

        assertThat(resultado.getEstado()).isEqualTo(EstadoAspirante.PENDIENTE);
        assertThat(aspirante.getEstado()).isEqualTo(EstadoAspirante.EN_CORRECCION);
        assertThat(aspirante.getMotivoDecision()).isEqualTo("Nombrar la razón social");
        verify(emailService).enviarCorreoHtml(eq("juan.romero@example.com"),
                eq("Tu solicitud necesita correcciones - PhotoBogota"), anyString());
    }

    @Test
    void enviarCredenciales_sinCuentaPrevia_creaCuentaDeSocioDesdeCero() {
        Aspirante aspirante = aspiranteDeEjemplo(EstadoAspirante.ENVIO_CREDENCIALES);
        when(aspiranteRepository.findById("asp-1")).thenReturn(Optional.of(aspirante));
        when(usuarioAuthRepository.findByEmail("juan.romero@example.com")).thenReturn(Optional.empty());
        when(usuarioAuthRepository.existsByNombreUsuario("juan.romero")).thenReturn(false);
        when(usuarioFactory.crearUsuario(any(ObjectId.class), any(), eq(Rol.SOCIO)))
                .thenReturn(Socio.builder().build());
        when(aspiranteMapper.toResponse(any(Aspirante.class))).thenReturn(dtoDeEjemplo());

        AspiranteResponseDTO resultado = aspiranteService.enviarCredenciales("asp-1", "mod1");

        assertThat(resultado.getId()).isEqualTo("asp-1");
        ArgumentCaptor<Aspirante> captor = ArgumentCaptor.forClass(Aspirante.class);
        verify(aspiranteRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoAspirante.APROBADO);
        assertThat(captor.getValue().getUsuarioCreadoId()).isNotBlank();
        assertThat(captor.getValue().getNombreUsuarioGenerado()).isEqualTo("juan.romero");
        verify(usuarioRepository).save(any(Usuario.class));
        verify(usuarioAuthRepository).save(any(UsuarioAuth.class));
        verify(emailService).enviarCorreoHtml(eq("juan.romero@example.com"),
                eq("Tus credenciales de acceso - PhotoBogota"), anyString());
    }

    @Test
    void enviarCredenciales_nombreUsuarioEnUso_agregaSufijoNumerico() {
        Aspirante aspirante = aspiranteDeEjemplo(EstadoAspirante.ENVIO_CREDENCIALES);
        when(aspiranteRepository.findById("asp-1")).thenReturn(Optional.of(aspirante));
        when(usuarioAuthRepository.findByEmail("juan.romero@example.com")).thenReturn(Optional.empty());
        when(usuarioAuthRepository.existsByNombreUsuario("juan.romero")).thenReturn(true);
        when(usuarioAuthRepository.existsByNombreUsuario("juan.romero1")).thenReturn(false);
        when(usuarioFactory.crearUsuario(any(ObjectId.class), any(), eq(Rol.SOCIO)))
                .thenReturn(Socio.builder().build());
        when(aspiranteMapper.toResponse(any(Aspirante.class))).thenReturn(dtoDeEjemplo());

        aspiranteService.enviarCredenciales("asp-1", "mod1");

        ArgumentCaptor<Aspirante> captor = ArgumentCaptor.forClass(Aspirante.class);
        verify(aspiranteRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getNombreUsuarioGenerado()).isEqualTo("juan.romero1");
    }

    @Test
    void enviarCredenciales_conCuentaMiembro_asciendeASocio() {
        Aspirante aspirante = aspiranteDeEjemplo(EstadoAspirante.ENVIO_CREDENCIALES);
        ObjectId usuarioId = new ObjectId();
        UsuarioAuth authMiembro = UsuarioAuth.builder()
                .id(usuarioId)
                .nombreUsuario("juanperez")
                .email("juan.romero@example.com")
                .rol(Rol.MIEMBRO)
                .build();
        Miembro base = Miembro.builder().id(usuarioId).nombresCompletos("Juan Romero").build();
        when(aspiranteRepository.findById("asp-1")).thenReturn(Optional.of(aspirante));
        when(usuarioAuthRepository.findByEmail("juan.romero@example.com")).thenReturn(Optional.of(authMiembro));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(base));
        when(aspiranteMapper.toResponse(any(Aspirante.class))).thenReturn(dtoDeEjemplo());

        aspiranteService.enviarCredenciales("asp-1", "mod1");

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertThat(usuarioCaptor.getValue()).isInstanceOf(Socio.class);
        assertThat(usuarioCaptor.getValue().getId()).isEqualTo(usuarioId);
        assertThat(authMiembro.getRol()).isEqualTo(Rol.SOCIO);
        assertThat(aspirante.getEstado()).isEqualTo(EstadoAspirante.APROBADO);
        verify(usuarioAuthRepository).save(authMiembro);
        verify(emailService).enviarCorreoHtml(eq("juan.romero@example.com"),
                eq("¡Ya eres socio de PhotoBogota!"), anyString());
    }

    @Test
    void enviarCredenciales_conCuentaDeOtroRol_lanzaOperacionInvalida() {
        Aspirante aspirante = aspiranteDeEjemplo(EstadoAspirante.ENVIO_CREDENCIALES);
        when(aspiranteRepository.findById("asp-1")).thenReturn(Optional.of(aspirante));
        when(usuarioAuthRepository.findByEmail("juan.romero@example.com"))
                .thenReturn(Optional.of(UsuarioAuth.builder()
                        .rol(Rol.SOCIO)
                        .build()));

        assertThatThrownBy(() -> aspiranteService.enviarCredenciales("asp-1", "mod1"))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("Revisa manualmente antes de continuar");
    }

    @Test
    void enviarCredenciales_estadoIncorrecto_lanzaOperacionInvalida() {
        Aspirante aspirante = aspiranteDeEjemplo(EstadoAspirante.PENDIENTE);
        when(aspiranteRepository.findById("asp-1")).thenReturn(Optional.of(aspirante));

        assertThatThrownBy(() -> aspiranteService.enviarCredenciales("asp-1", "mod1"))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("Solo se pueden enviar credenciales");
    }

    @Test
    void enviarCredenciales_yaEnviadas_lanzaOperacionInvalida() {
        Aspirante aspirante = aspiranteDeEjemplo(EstadoAspirante.ENVIO_CREDENCIALES);
        aspirante.setUsuarioCreadoId("jsdflasdf");
        when(aspiranteRepository.findById("asp-1")).thenReturn(Optional.of(aspirante));

        assertThatThrownBy(() -> aspiranteService.enviarCredenciales("asp-1", "mod1"))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("Ya se enviaron las credenciales");
    }

    @Test
    void reenviarDocumentos_enCorreccion_vuelveAPendienteYCuentaLaCorreccion() {
        Aspirante aspirante = aspiranteDeEjemplo(EstadoAspirante.EN_CORRECCION);
        when(aspiranteRepository.findByCodigo("SOL-123456")).thenReturn(Optional.of(aspirante));
        when(aspiranteRepository.save(any(Aspirante.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aspiranteMapper.toResponse(any(Aspirante.class))).thenReturn(dtoDeEjemplo());

        ReenvioDocumentosDTO request = ReenvioDocumentosDTO.builder()
                .rutaArchivo("/uploads/cedula_corregida.pdf")
                .tipoArchivo("pdf")
                .build();

        AspiranteResponseDTO resultado = aspiranteService.reenviarDocumentos("SOL-123456", request);

        assertThat(resultado.getEstado()).isEqualTo(EstadoAspirante.PENDIENTE);
        assertThat(aspirante.getRutaArchivo()).isEqualTo("/uploads/cedula_corregida.pdf");
        assertThat(aspirante.getVecesCorregida()).isEqualTo(1);
        assertThat(aspirante.getEstado()).isEqualTo(EstadoAspirante.PENDIENTE);
        assertThat(aspirante.getFechaReenvio()).isNotNull();
        verify(emailService).enviarCorreoHtml(eq("juan.romero@example.com"),
                eq("Recibimos tus documentos - PhotoBogota"), anyString());
    }

    @Test
    void reenviarDocumentos_estadoIncorrecto_lanzaOperacionInvalida() {
        Aspirante aspirante = aspiranteDeEjemplo(EstadoAspirante.PENDIENTE);
        when(aspiranteRepository.findByCodigo("SOL-123456")).thenReturn(Optional.of(aspirante));

        ReenvioDocumentosDTO request = ReenvioDocumentosDTO.builder()
                .rutaArchivo("/uploads/cedula.pdf")
                .build();

        assertThatThrownBy(() -> aspiranteService.reenviarDocumentos("SOL-123456", request))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("Solo puedes reenviar documentos");
    }

    @Test
    void agregarComentarioInterno_anadeComentarioALaBitacora() {
        Aspirante aspirante = aspiranteDeEjemplo(EstadoAspirante.PENDIENTE);
        when(aspiranteRepository.findById("asp-1")).thenReturn(Optional.of(aspirante));
        when(aspiranteRepository.save(any(Aspirante.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aspiranteMapper.toResponse(any(Aspirante.class))).thenReturn(dtoDeEjemplo());

        AspiranteResponseDTO resultado = aspiranteService.agregarComentarioInterno("asp-1", "Parece legítimo", "mod1");

        assertThat(resultado.getId()).isEqualTo("asp-1");
        assertThat(aspirante.getComentariosInternos()).hasSize(1);
        assertThat(aspirante.getComentariosInternos().get(0).getAutor()).isEqualTo("mod1");
        assertThat(aspirante.getComentariosInternos().get(0).getTexto()).isEqualTo("Parece legítimo");
    }

    @Test
    void obtenerEstadisticas_cuentaCadaEstado() {
        when(aspiranteRepository.findAll()).thenReturn(List.of(
                aspiranteDeEjemplo(EstadoAspirante.PENDIENTE),
                aspiranteDeEjemplo(EstadoAspirante.EN_CORRECCION),
                aspiranteDeEjemplo(EstadoAspirante.ENVIO_CREDENCIALES),
                aspiranteDeEjemplo(EstadoAspirante.APROBADO),
                aspiranteDeEjemplo(EstadoAspirante.RECHAZADO)));

        EstadisticasAspiranteDTO estadisticas = aspiranteService.obtenerEstadisticas();

        assertThat(estadisticas.getTotal()).isEqualTo(5);
        assertThat(estadisticas.getPendientes()).isEqualTo(1);
        assertThat(estadisticas.getEnCorreccion()).isEqualTo(1);
        assertThat(estadisticas.getEnEnvioCredenciales()).isEqualTo(1);
        assertThat(estadisticas.getAprobadas()).isEqualTo(1);
        assertThat(estadisticas.getRechazadas()).isEqualTo(1);
        assertThat(estadisticas.getProcesadas()).isEqualTo(3);
    }

    @Test
    void actualizarEstado_diferente_guardaElNuevoEstado() {
        Aspirante aspirante = aspiranteDeEjemplo(EstadoAspirante.PENDIENTE);
        when(aspiranteRepository.findById("asp-1")).thenReturn(Optional.of(aspirante));
        when(aspiranteRepository.save(any(Aspirante.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aspiranteMapper.toResponse(any(Aspirante.class))).thenReturn(dtoDeEjemplo());

        AspiranteResponseDTO resultado = aspiranteService.actualizarEstado("asp-1", EstadoAspirante.EN_CORRECCION);

        assertThat(aspirante.getEstado()).isEqualTo(EstadoAspirante.EN_CORRECCION);
        verify(aspiranteRepository).save(aspirante);
    }

    @Test
    void actualizarEstado_igual_lanzaOperacionInvalida() {
        Aspirante aspirante = aspiranteDeEjemplo(EstadoAspirante.PENDIENTE);
        when(aspiranteRepository.findById("asp-1")).thenReturn(Optional.of(aspirante));

        assertThatThrownBy(() -> aspiranteService.actualizarEstado("asp-1", EstadoAspirante.PENDIENTE))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("El aspirante ya se encuentra en estado");
    }
}