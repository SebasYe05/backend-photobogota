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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.photobogota.api.dto.DependenciasEliminacionDTO;
import com.photobogota.api.dto.MetricasEliminacionDTO;
import com.photobogota.api.dto.ProcesarEliminacionAdminDTO;
import com.photobogota.api.dto.SolicitudEliminacionAdminDTO;
import com.photobogota.api.exception.OperacionInvalidaException;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.model.EstadoReporte;
import com.photobogota.api.model.EstadoSolicitudEliminacion;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.MotivoEliminacionCuenta;
import com.photobogota.api.model.Notificacion;
import com.photobogota.api.model.NotificacionTipo;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.Reporte;
import com.photobogota.api.model.SolicitudEliminacionCuenta;
import com.photobogota.api.model.Spot;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.CodigoEliminacionRepository;
import com.photobogota.api.repository.NotificacionRepository;
import com.photobogota.api.repository.RefreshTokenRepository;
import com.photobogota.api.repository.ReporteRepository;
import com.photobogota.api.repository.SesionRepository;
import com.photobogota.api.repository.SolicitudEliminacionRepository;
import com.photobogota.api.repository.SpotRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminEliminacionCuentaServiceImplTest {

    @Mock
    private SolicitudEliminacionRepository solicitudEliminacionRepository;

    @Mock
    private UsuarioAuthRepository usuarioAuthRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private ReporteRepository reporteRepository;

    @Mock
    private NotificacionRepository notificacionRepository;

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
    private AdminEliminacionCuentaServiceImpl servicio;

    private static final ObjectId SOLICITUD_ID = new ObjectId();
    private static final ObjectId USUARIO_ID = new ObjectId();
    private static final String EMAIL = "juan@mail.com";
    private static final String NOMBRE = "juan";
    private static final String ADMIN = "admin1";

    private SolicitudEliminacionCuenta solicitud(EstadoSolicitudEliminacion estado) {
        return SolicitudEliminacionCuenta.builder()
                .id(SOLICITUD_ID)
                .usuarioId(USUARIO_ID)
                .email(EMAIL)
                .nombreUsuario(NOMBRE)
                .motivo(MotivoEliminacionCuenta.PREOCUPACIONES_DE_PRIVACIDAD)
                .comentario("Comentario de prueba")
                .estado(estado)
                .fechaSolicitud(LocalDateTime.now().minusDays(1))
                .build();
    }

    private UsuarioAuth authCoincidente() {
        return UsuarioAuth.builder()
                .id(USUARIO_ID)
                .email(EMAIL)
                .nombreUsuario(NOMBRE)
                .contrasena("pass")
                .rol(Rol.MIEMBRO)
                .build();
    }

    private Miembro perfil() {
        return Miembro.builder()
                .id(USUARIO_ID)
                .nombresCompletos("Juan Pérez")
                .estadoCuenta(true)
                .build();
    }

    private void stubBusqueda(SolicitudEliminacionCuenta solicitud) {
        when(solicitudEliminacionRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));
    }

    @Test
    void listarSolicitudes_conEstado_devuelvePaginaMapeada() {
        Pageable pageable = PageRequest.of(0, 10);
        when(solicitudEliminacionRepository.findByEstado(EstadoSolicitudEliminacion.PENDIENTE_VERIFICACION,
                pageable))
                .thenReturn(new PageImpl<>(List.of(solicitud(EstadoSolicitudEliminacion.PENDIENTE_VERIFICACION)),
                        pageable, 1));

        Page<SolicitudEliminacionAdminDTO> pagina = servicio.listarSolicitudes(
                EstadoSolicitudEliminacion.PENDIENTE_VERIFICACION, pageable);

        assertThat(pagina.getContent()).hasSize(1);
        SolicitudEliminacionAdminDTO dto = pagina.getContent().get(0);
        assertThat(dto.getId()).isEqualTo(SOLICITUD_ID.toHexString());
        assertThat(dto.getRol()).isEqualTo("DESCONOCIDO");
        assertThat(dto.isIdentidadVerificada()).isFalse();
        assertThat(dto.getDependencias().isTieneDependenciasPendientes()).isFalse();
    }

    @Test
    void listarSolicitudes_sinEstado_usaFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        when(solicitudEliminacionRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(solicitud(EstadoSolicitudEliminacion.PROGRAMADA)),
                        pageable, 1));

        Page<SolicitudEliminacionAdminDTO> pagina = servicio.listarSolicitudes(null, pageable);

        assertThat(pagina.getContent()).hasSize(1);
    }

    @Test
    void obtenerDetalle_conSolicitudValida_construyeDTO() {
        stubBusqueda(solicitud(EstadoSolicitudEliminacion.PROGRAMADA));

        SolicitudEliminacionAdminDTO dto = servicio.obtenerDetalle(SOLICITUD_ID.toHexString());

        assertThat(dto.getId()).isEqualTo(SOLICITUD_ID.toHexString());
        assertThat(dto.getUsuarioId()).isEqualTo(USUARIO_ID.toHexString());
        assertThat(dto.getRol()).isEqualTo("DESCONOCIDO");
    }

    @Test
    void obtenerDetalle_idInvalido_lanzaOperacionInvalida() {
        assertThatThrownBy(() -> servicio.obtenerDetalle("id-no-valido"))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("no es válido");
    }

    @Test
    void obtenerDetalle_noEncontrada_lanzaResourceNotFound() {
        when(solicitudEliminacionRepository.findById(SOLICITUD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerDetalle(SOLICITUD_ID.toHexString()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No se encontró la solicitud");
    }

    @Test
    void procesarInmediatamente_solicitudValida_completaYAnonimiza() {
        SolicitudEliminacionCuenta solicitud = solicitud(EstadoSolicitudEliminacion.PENDIENTE_VERIFICACION);
        stubBusqueda(solicitud);
        UsuarioAuth auth = authCoincidente();
        when(usuarioAuthRepository.findById(USUARIO_ID)).thenReturn(Optional.of(auth));
        Miembro perfil = perfil();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(perfil));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(emailService.construirHtmlEliminacionCompletada(NOMBRE)).thenReturn("html");

        ProcesarEliminacionAdminDTO dto = ProcesarEliminacionAdminDTO.builder()
                .observacion("Solicitud revisada").build();
        String resultado = servicio.procesarInmediatamente(SOLICITUD_ID.toHexString(), ADMIN, dto);

        assertThat(resultado).contains("anonimizada");
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudEliminacion.COMPLETADA);
        assertThat(solicitud.getProcesadaPorAdmin()).isEqualTo(ADMIN);
        assertThat(solicitud.getObservacionAdmin()).isEqualTo("Solicitud revisada");
        assertThat(perfil.getEstadoCuenta()).isFalse();
        assertThat(perfil.getNombresCompletos()).isEqualTo("Usuario eliminado");
        assertThat(auth.getEmail()).endsWith("@anonimizado.photobogota.com");
        verify(usuarioRepository).save(perfil);
        verify(usuarioAuthRepository).save(auth);
        verify(refreshTokenRepository).deleteByEmail(EMAIL);
        verify(sesionRepository).deleteByUsuarioId(USUARIO_ID.toString());
        verify(emailService).enviarCorreoHtml(EMAIL, "Tu cuenta fue eliminada - PhotoBogota", "html");
        verify(solicitudEliminacionRepository).save(solicitud);
    }

    @Test
    void procesarInmediatamente_solicitudCompletada_lanzaOperacionInvalida() {
        stubBusqueda(solicitud(EstadoSolicitudEliminacion.COMPLETADA));

        assertThatThrownBy(() -> servicio.procesarInmediatamente(
                SOLICITUD_ID.toHexString(), ADMIN, null))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("completada previamente");
    }

    @Test
    void procesarInmediatamente_usuarioNoExiste_lanzaResourceNotFound() {
        stubBusqueda(solicitud(EstadoSolicitudEliminacion.PENDIENTE_VERIFICACION));
        when(usuarioAuthRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.procesarInmediatamente(
                SOLICITUD_ID.toHexString(), ADMIN, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ya no existe");
    }

    @Test
    void procesarInmediatamente_identidadNoCoincide_lanzaOperacionInvalida() {
        stubBusqueda(solicitud(EstadoSolicitudEliminacion.PENDIENTE_VERIFICACION));
        UsuarioAuth auth = UsuarioAuth.builder()
                .id(USUARIO_ID)
                .email("otro@mail.com")
                .nombreUsuario("otro")
                .rol(Rol.MIEMBRO)
                .build();
        when(usuarioAuthRepository.findById(USUARIO_ID)).thenReturn(Optional.of(auth));

        assertThatThrownBy(() -> servicio.procesarInmediatamente(
                SOLICITUD_ID.toHexString(), ADMIN, null))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("No se pudo verificar");
    }

    @Test
    void procesarInmediatamente_conDependencias_cierraReportesYNotifica() {
        SolicitudEliminacionCuenta solicitud = solicitud(EstadoSolicitudEliminacion.PENDIENTE_VERIFICACION);
        stubBusqueda(solicitud);
        when(usuarioAuthRepository.findById(USUARIO_ID)).thenReturn(Optional.of(authCoincidente()));

        Spot spot = new Spot();
        spot.setId("spot-1");
        spot.setCreadorUsername(NOMBRE);
        when(spotRepository.findByCreadorUsername(NOMBRE)).thenReturn(List.of(spot));

        Reporte reporte = Reporte.builder()
                .id("r1")
                .numeroTicket("REP-123")
                .reportadoPor("otro")
                .estado(EstadoReporte.NUEVO)
                .build();
        when(reporteRepository.findBySpotIdInAndEstadoIn(anyList(), anyList()))
                .thenReturn(List.of(reporte));

        Miembro perfil = perfil();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(perfil));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(emailService.construirHtmlEliminacionCompletada(NOMBRE)).thenReturn("html");

        servicio.procesarInmediatamente(SOLICITUD_ID.toHexString(), ADMIN, null);

        assertThat(reporte.getEstado()).isEqualTo(EstadoReporte.RESUELTO);
        assertThat(reporte.getActualizadoPor()).isEqualTo(ADMIN);
        assertThat(reporte.getBitacora()).hasSize(1);
        assertThat(reporte.getBitacora().get(0).getAutor()).isEqualTo(ADMIN);
        verify(reporteRepository).save(reporte);

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        Notificacion notificacion = captor.getValue();
        assertThat(notificacion.getDestinatarioUsername()).isEqualTo("otro");
        assertThat(notificacion.getTipo()).isEqualTo(NotificacionTipo.SISTEMA);
        assertThat(notificacion.getEmisorUsername()).isEqualTo("sistema");

        assertThat(spot.getCreadorUsername()).startsWith("usuario_eliminado_");
        verify(spotRepository).save(spot);
    }

    @Test
    void rechazarSolicitud_solicitudActiva_cancelaYReactivaCuenta() {
        SolicitudEliminacionCuenta solicitud = solicitud(EstadoSolicitudEliminacion.PENDIENTE_VERIFICACION);
        stubBusqueda(solicitud);
        Miembro perfil = perfil();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(perfil));
        when(emailService.construirHtmlCancelacionEliminacion(NOMBRE)).thenReturn("html");

        String resultado = servicio.rechazarSolicitud(SOLICITUD_ID.toHexString(), ADMIN, null);

        assertThat(resultado).contains("rechazada");
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudEliminacion.CANCELADA);
        assertThat(solicitud.getProcesadaPorAdmin()).isEqualTo(ADMIN);
        assertThat(solicitud.getFechaCancelacion()).isNotNull();
        assertThat(perfil.getEstadoCuenta()).isTrue();
        verify(solicitudEliminacionRepository).save(solicitud);
        verify(usuarioRepository).save(perfil);
        verify(codigoEliminacionRepository).deleteByEmail(EMAIL);
        verify(emailService).enviarCorreoHtml(EMAIL, "Tu cuenta fue reactivada - PhotoBogota", "html");
    }

    @Test
    void rechazarSolicitud_estadoNoActivo_lanzaOperacionInvalida() {
        stubBusqueda(solicitud(EstadoSolicitudEliminacion.COMPLETADA));

        assertThatThrownBy(() -> servicio.rechazarSolicitud(SOLICITUD_ID.toHexString(), ADMIN, null))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("solicitudes activas");
    }

    @Test
    void obtenerMetricas_agrupaPorEstadoMotivoYRol() {
        SolicitudEliminacionCuenta completada = solicitud(EstadoSolicitudEliminacion.COMPLETADA);
        completada.setFechaConfirmacion(LocalDateTime.now().minusDays(5));
        completada.setFechaCompletada(LocalDateTime.now());
        completada.setProcesadaManualmente(true);

        SolicitudEliminacionCuenta pendiente = SolicitudEliminacionCuenta.builder()
                .id(new ObjectId())
                .usuarioId(new ObjectId())
                .email("otro@mail.com")
                .nombreUsuario("otro")
                .motivo(MotivoEliminacionCuenta.PREOCUPACIONES_DE_PRIVACIDAD)
                .estado(EstadoSolicitudEliminacion.PENDIENTE_VERIFICACION)
                .build();

        when(solicitudEliminacionRepository.findAll()).thenReturn(List.of(completada, pendiente));
        UsuarioAuth authMiembro = UsuarioAuth.builder()
                .id(completada.getUsuarioId())
                .email(EMAIL)
                .nombreUsuario(NOMBRE)
                .rol(Rol.MIEMBRO)
                .build();
        when(usuarioAuthRepository.findById(completada.getUsuarioId()))
                .thenReturn(Optional.of(authMiembro));

        MetricasEliminacionDTO metricas = servicio.obtenerMetricas();

        assertThat(metricas.getTotalSolicitudes()).isEqualTo(2);
        assertThat(metricas.getPorEstado()).containsEntry("COMPLETADA", 1L);
        assertThat(metricas.getPorEstado()).containsEntry("PENDIENTE_VERIFICACION", 1L);
        assertThat(metricas.getPorRol()).containsEntry("MIEMBRO", 1L);
        assertThat(metricas.getPorRol()).containsEntry("DESCONOCIDO", 1L);
        assertThat(metricas.getPorMotivo()).containsEntry("PREOCUPACIONES_DE_PRIVACIDAD", 2L);
        assertThat(metricas.getPromedioDiasHastaCompletada()).isEqualTo(5.0);
        assertThat(metricas.getCompletadasUltimos30Dias()).isEqualTo(1L);
        assertThat(metricas.getProcesadasManualmentePorAdmin()).isEqualTo(1L);
    }

    @Test
    void obtenerMetricas_sinSolicitudes_devuelveVacios() {
        when(solicitudEliminacionRepository.findAll()).thenReturn(List.of());

        MetricasEliminacionDTO metricas = servicio.obtenerMetricas();

        assertThat(metricas.getTotalSolicitudes()).isZero();
        assertThat(metricas.getPorEstado()).isEmpty();
        assertThat(metricas.getPorMotivo()).isEmpty();
        assertThat(metricas.getPorRol()).isEmpty();
        assertThat(metricas.getPromedioDiasHastaCompletada()).isNull();
    }
}