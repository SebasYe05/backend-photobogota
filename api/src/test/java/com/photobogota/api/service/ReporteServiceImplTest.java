package com.photobogota.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.photobogota.api.dto.CambiarEstadoRequestDTO;
import com.photobogota.api.dto.CrearReporteRequestDTO;
import com.photobogota.api.dto.EscalarReporteRequestDTO;
import com.photobogota.api.dto.ReporteResponseDTO;
import com.photobogota.api.dto.ValidarReporteRequestDTO;
import com.photobogota.api.exception.AccessForbiddenException;
import com.photobogota.api.exception.OperacionInvalidaException;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.model.Calificacion;
import com.photobogota.api.model.CategoriaReporte;
import com.photobogota.api.model.EstadoReporte;
import com.photobogota.api.model.Gravedad;
import com.photobogota.api.model.Reporte;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.Spot;
import com.photobogota.api.model.TipoObjetivoReporte;
import com.photobogota.api.repository.CalificacionRepository;
import com.photobogota.api.repository.ReporteRepository;
import com.photobogota.api.repository.SpotRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceImplTest {

    @Mock
    private ReporteRepository reporteRepository;

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private CalificacionRepository calificacionRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private IPuntosService puntosService;

    @Mock
    private INotificacionService notificacionService;

    @InjectMocks
    private ReporteServiceImpl reporteService;

    private CrearReporteRequestDTO requestReporte() {
        CrearReporteRequestDTO request = new CrearReporteRequestDTO();
        request.setSpotId("spot-1");
        request.setResenaId("resena-1");
        request.setCategoria(CategoriaReporte.PROBLEMA_SPOT);
        request.setDescripcion("Foto desactualizada del local");
        request.setEvidencias(List.of("/reportes/evidencia-1.jpg"));
        return request;
    }

    private Spot spotDeEjemplo() {
        Spot spot = new Spot();
        spot.setId("spot-1");
        spot.setNombre("Caldos Doña Gloria");
        spot.setCreadorRol("SOCIO");
        spot.setCreadorUsername("socio1");
        return spot;
    }

    private Calificacion calificacionDeEjemplo() {
        return Calificacion.builder()
                .id("resena-1")
                .spotId("spot-1")
                .usuario("miembro1")
                .build();
    }

    private Reporte reporteDeEjemplo() {
        return Reporte.builder()
                .id("rep-1")
                .numeroTicket("REP-000001")
                .categoria(CategoriaReporte.PROBLEMA_SPOT)
                .descripcion("Descripción del reporte")
                .reportadoPor("miembro1")
                .tipoObjetivo(TipoObjetivoReporte.SPOT)
                .spotId("spot-1")
                .nombreSpot("Caldos Doña Gloria")
                .asignadoA(Rol.MOD)
                .gravedad(Gravedad.MEDIA)
                .estado(EstadoReporte.NUEVO)
                .fechaCreacion(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
    }

    @Test
    void crearReporte_conResena_validaReincidenciaYAsignaResponsable() {
        when(spotRepository.findById("spot-1")).thenReturn(Optional.of(spotDeEjemplo()));
        when(calificacionRepository.findById("resena-1")).thenReturn(Optional.of(calificacionDeEjemplo()));
        when(reporteRepository.countByResenaIdAndEstadoIn(eq("resena-1"), anyList())).thenReturn(1L);
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));

        ReporteResponseDTO resultado = reporteService.crearReporte(requestReporte(), "miembro2");

        assertThat(resultado.getTipoObjetivo()).isEqualTo(TipoObjetivoReporte.RESENA);
        assertThat(resultado.getEstado()).isEqualTo(EstadoReporte.NUEVO);
        assertThat(resultado.getAsignadoA()).isEqualTo(Rol.MOD);
        assertThat(resultado.getGravedad()).isEqualTo(Gravedad.ALTA);
        assertThat(resultado.getNombreSpot()).isEqualTo("Caldos Doña Gloria");
        assertThat(resultado.getEsLocalDeSocio()).isTrue();
    }

    @Test
    void crearReporte_sinReincidencia_usaGravedadBase() {
        when(spotRepository.findById("spot-1")).thenReturn(Optional.of(spotDeEjemplo()));
        when(calificacionRepository.findById("resena-1")).thenReturn(Optional.of(calificacionDeEjemplo()));
        when(reporteRepository.countByResenaIdAndEstadoIn(eq("resena-1"), anyList())).thenReturn(0L);
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));

        ReporteResponseDTO resultado = reporteService.crearReporte(requestReporte(), "miembro2");

        assertThat(resultado.getGravedad()).isEqualTo(Gravedad.MEDIA);
    }

    @Test
    void crearReporte_reincidenciaAlta_subioOrtica() {
        when(spotRepository.findById("spot-1")).thenReturn(Optional.of(spotDeEjemplo()));
        when(calificacionRepository.findById("resena-1")).thenReturn(Optional.of(calificacionDeEjemplo()));
        when(reporteRepository.countByResenaIdAndEstadoIn(eq("resena-1"), anyList())).thenReturn(3L);
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));

        ReporteResponseDTO resultado = reporteService.crearReporte(requestReporte(), "miembro2");

        assertThat(resultado.getGravedad()).isEqualTo(Gravedad.CRITICA);
    }

    @Test
    void crearReporte_errorTecnico_seAsignaAAdmin() {
        when(spotRepository.findById("spot-1")).thenReturn(Optional.of(spotDeEjemplo()));
        when(calificacionRepository.findById("resena-1")).thenReturn(Optional.of(calificacionDeEjemplo()));
        when(reporteRepository.countByResenaIdAndEstadoIn(eq("resena-1"), anyList())).thenReturn(0L);
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));

        CrearReporteRequestDTO request = requestReporte();
        request.setCategoria(CategoriaReporte.ERROR_TECNICO);

        ReporteResponseDTO resultado = reporteService.crearReporte(request, "miembro2");

        assertThat(resultado.getAsignadoA()).isEqualTo(Rol.ADMIN);
        assertThat(resultado.getGravedad()).isEqualTo(Gravedad.ALTA);
    }

    @Test
    void crearReporte_resenaDeOtroSpot_lanzaResourceNotFound() {
        Calificacion otraResena = calificacionDeEjemplo();
        otraResena.setSpotId("spot-2");
        when(spotRepository.findById("spot-1")).thenReturn(Optional.of(spotDeEjemplo()));
        when(calificacionRepository.findById("resena-1")).thenReturn(Optional.of(otraResena));

        assertThatThrownBy(() -> reporteService.crearReporte(requestReporte(), "miembro2"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no pertenece al spot");
    }

    @Test
    void crearReporte_autorDeSuPropiaResena_lanzaAccessForbidden() {
        when(spotRepository.findById("spot-1")).thenReturn(Optional.of(spotDeEjemplo()));
        when(calificacionRepository.findById("resena-1")).thenReturn(Optional.of(calificacionDeEjemplo()));

        assertThatThrownBy(() -> reporteService.crearReporte(requestReporte(), "miembro1"))
                .isInstanceOf(AccessForbiddenException.class)
                .hasMessageContaining("No puedes reportar tu propia reseña");
    }

    @Test
    void crearReporte_spotInexistente_lanzaResourceNotFound() {
        when(spotRepository.findById("spot-x")).thenReturn(Optional.empty());

        CrearReporteRequestDTO request = requestReporte();
        request.setSpotId("spot-x");

        assertThatThrownBy(() -> reporteService.crearReporte(request, "miembro1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crearReporte_resenaSinSpotId_lanzaResourceNotFound() {
        CrearReporteRequestDTO request = requestReporte();
        request.setSpotId(null);

        assertThatThrownBy(() -> reporteService.crearReporte(request, "miembro1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Debes indicar el spotId");
    }

    @Test
    void obtenerPorId_existente_devuelveDto() {
        when(reporteRepository.findById("rep-1")).thenReturn(Optional.of(reporteDeEjemplo()));

        ReporteResponseDTO resultado = reporteService.obtenerPorId("rep-1");

        assertThat(resultado.getNumeroTicket()).isEqualTo("REP-000001");
        assertThat(resultado.getEstado()).isEqualTo(EstadoReporte.NUEVO);
    }

    @Test
    void obtenerPorId_inexistente_lanzaResourceNotFound() {
        when(reporteRepository.findById("rep-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reporteService.obtenerPorId("rep-x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarMisReportes_devuelveLosDelUsuario() {
        when(reporteRepository.findByReportadoPor("miembro1")).thenReturn(List.of(reporteDeEjemplo()));

        List<ReporteResponseDTO> resultado = reporteService.listarMisReportes("miembro1");

        assertThat(resultado).hasSize(1);
    }

    @Test
    void listarPorRolAsignado_devuelveLosDeLaColaDelRol() {
        when(reporteRepository.findByAsignadoA(Rol.MOD)).thenReturn(List.of(reporteDeEjemplo()));

        List<ReporteResponseDTO> resultado = reporteService.listarPorRolAsignado(Rol.MOD);

        assertThat(resultado).hasSize(1);
    }

    @Test
    void obtenerDashboard_socioConSpots_filtraPorSusLocales() {
        when(spotRepository.findByCreadorUsername("socio1")).thenReturn(List.of(spotDeEjemplo()));
        when(mongoTemplate.find(any(Query.class), eq(Reporte.class))).thenReturn(List.of(reporteDeEjemplo()));

        List<ReporteResponseDTO> resultado = reporteService.obtenerDashboard(
                Rol.SOCIO, "socio1", null, null, null, null, null, null);

        assertThat(resultado).hasSize(1);
    }

    @Test
    void obtenerDashboard_socioSinSpots_devuelveListaVacia() {
        when(spotRepository.findByCreadorUsername("socioX")).thenReturn(List.of());

        List<ReporteResponseDTO> resultado = reporteService.obtenerDashboard(
                Rol.SOCIO, "socioX", null, null, null, null, null, null);

        assertThat(resultado).isEmpty();
    }

    @Test
    void obtenerDashboard_adminFiltraPorAsignacionYOrdenaRecientes() {
        Reporte antiguo = reporteDeEjemplo();
        Reporte reciente = reporteDeEjemplo();
        reciente.setId("rep-2");
        antiguo.setFechaCreacion(LocalDateTime.of(2026, 1, 1, 10, 0));
        reciente.setFechaCreacion(LocalDateTime.of(2026, 1, 2, 10, 0));
        when(mongoTemplate.find(any(Query.class), eq(Reporte.class))).thenReturn(List.of(antiguo, reciente));

        List<ReporteResponseDTO> resultado = reporteService.obtenerDashboard(
                Rol.ADMIN, "admin", null, null, null, null, null, "recientes");

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getId()).isEqualTo("rep-2");
    }

    @Test
    void cambiarEstado_modResuelve_deInmediatoYotorgaPuntos() {
        Reporte reporte = reporteDeEjemplo();
        when(reporteRepository.findById("rep-1")).thenReturn(Optional.of(reporte));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));

        CambiarEstadoRequestDTO request = new CambiarEstadoRequestDTO();
        request.setEstado(EstadoReporte.RESUELTO);

        ReporteResponseDTO resultado = reporteService.cambiarEstado("rep-1", request, "mod1", Rol.MOD);

        assertThat(resultado.getEstado()).isEqualTo(EstadoReporte.RESUELTO);
        verify(puntosService).sumarPuntos(eq("miembro1"), any(), eq("rep-1"));
    }

    @Test
    void cambiarEstado_socioResuelve_pasaPendienteValidacionSinPuntos() {
        Reporte reporte = reporteDeEjemplo();
        reporte.setSpotId("spot-1");
        when(reporteRepository.findById("rep-1")).thenReturn(Optional.of(reporte));
        when(spotRepository.findById("spot-1")).thenReturn(Optional.of(spotDeEjemplo()));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));

        CambiarEstadoRequestDTO request = new CambiarEstadoRequestDTO();
        request.setEstado(EstadoReporte.RESUELTO);

        ReporteResponseDTO resultado = reporteService.cambiarEstado("rep-1", request, "socio1", Rol.SOCIO);

        assertThat(resultado.getEstado()).isEqualTo(EstadoReporte.PENDIENTE_VALIDACION);
        verify(puntosService, never()).sumarPuntos(any(), any(), any());
    }

    @Test
    void cambiarEstado_modConReporteNoAsignado_lanzaAccessForbidden() {
        Reporte reporte = reporteDeEjemplo();
        reporte.setAsignadoA(Rol.ADMIN);
        when(reporteRepository.findById("rep-1")).thenReturn(Optional.of(reporte));

        CambiarEstadoRequestDTO request = new CambiarEstadoRequestDTO();
        request.setEstado(EstadoReporte.EN_REVISION);

        assertThatThrownBy(() -> reporteService.cambiarEstado("rep-1", request, "mod1", Rol.MOD))
                .isInstanceOf(AccessForbiddenException.class);
    }

    @Test
    void cambiarEstado_socioDeOtroLocal_lanzaAccessForbidden() {
        Reporte reporte = reporteDeEjemplo();
        reporte.setSpotId("spot-2");
        when(reporteRepository.findById("rep-1")).thenReturn(Optional.of(reporte));
        when(spotRepository.findById("spot-2")).thenReturn(Optional.empty());

        CambiarEstadoRequestDTO request = new CambiarEstadoRequestDTO();
        request.setEstado(EstadoReporte.EN_REVISION);

        assertThatThrownBy(() -> reporteService.cambiarEstado("rep-1", request, "socio1", Rol.SOCIO))
                .isInstanceOf(AccessForbiddenException.class);
    }

    @Test
    void cambiarEstado_conObservacion_agregaEntradaABitacora() {
        Reporte reporte = reporteDeEjemplo();
        when(reporteRepository.findById("rep-1")).thenReturn(Optional.of(reporte));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));

        CambiarEstadoRequestDTO request = new CambiarEstadoRequestDTO();
        request.setEstado(EstadoReporte.EN_REVISION);
        request.setObservacion("Solicitando más evidencia");

        ReporteResponseDTO resultado = reporteService.cambiarEstado("rep-1", request, "mod1", Rol.MOD);

        assertThat(resultado.getBitacora()).hasSize(1);
        assertThat(resultado.getBitacora().get(0).getAutor()).isEqualTo("mod1");
    }

    @Test
    void escalarReporte_mod_asignaAdminYsubeGravedadACritica() {
        Reporte reporte = reporteDeEjemplo();
        when(reporteRepository.findById("rep-1")).thenReturn(Optional.of(reporte));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));

        EscalarReporteRequestDTO request = new EscalarReporteRequestDTO();
        request.setMotivo("Requiere intervención de administradores");

        ReporteResponseDTO resultado = reporteService.escalarReporte("rep-1", request, "mod1", Rol.MOD);

        assertThat(resultado.getAsignadoA()).isEqualTo(Rol.ADMIN);
        assertThat(resultado.getGravedad()).isEqualTo(Gravedad.CRITICA);
        assertThat(resultado.getEscalado()).isTrue();
        assertThat(resultado.getEscaladoPor()).isEqualTo("mod1");
    }

    @Test
    void escalarReporte_socio_asignaMod() {
        Reporte reporte = reporteDeEjemplo();
        reporte.setSpotId("spot-1");
        when(reporteRepository.findById("rep-1")).thenReturn(Optional.of(reporte));
        when(spotRepository.findById("spot-1")).thenReturn(Optional.of(spotDeEjemplo()));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));

        EscalarReporteRequestDTO request = new EscalarReporteRequestDTO();
        request.setMotivo("El local fue reincidente");

        ReporteResponseDTO resultado = reporteService.escalarReporte("rep-1", request, "socio1", Rol.SOCIO);

        assertThat(resultado.getAsignadoA()).isEqualTo(Rol.MOD);
        assertThat(resultado.getEscalado()).isTrue();
    }

    @Test
    void escalarReporte_admin_lanzaAccessForbidden() {
        when(reporteRepository.findById("rep-1")).thenReturn(Optional.of(reporteDeEjemplo()));

        EscalarReporteRequestDTO request = new EscalarReporteRequestDTO();
        request.setMotivo("Escalamiento");

        assertThatThrownBy(() -> reporteService.escalarReporte("rep-1", request, "admin", Rol.ADMIN))
                .isInstanceOf(AccessForbiddenException.class);
        verify(reporteRepository, never()).save(any());
    }

    @Test
    void escalarReporte_yaEscalado_lanzaOperacionInvalida() {
        Reporte reporte = reporteDeEjemplo();
        reporte.setEscalado(true);
        when(reporteRepository.findById("rep-1")).thenReturn(Optional.of(reporte));

        EscalarReporteRequestDTO request = new EscalarReporteRequestDTO();
        request.setMotivo("Nuevo escalamiento");

        assertThatThrownBy(() -> reporteService.escalarReporte("rep-1", request, "mod1", Rol.MOD))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void listarPendientesValidacion_devuelveSoloEseEstado() {
        Reporte pendiente = reporteDeEjemplo();
        pendiente.setEstado(EstadoReporte.PENDIENTE_VALIDACION);
        when(reporteRepository.findByEstado(EstadoReporte.PENDIENTE_VALIDACION)).thenReturn(List.of(pendiente));

        List<ReporteResponseDTO> resultado = reporteService.listarPendientesValidacion();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstado()).isEqualTo(EstadoReporte.PENDIENTE_VALIDACION);
    }

    @Test
    void validarReporte_aprobado_marcaResueltoNotificaYotorgaPuntos() {
        Reporte reporte = reporteDeEjemplo();
        reporte.setEstado(EstadoReporte.PENDIENTE_VALIDACION);
        when(reporteRepository.findById("rep-1")).thenReturn(Optional.of(reporte));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));

        ValidarReporteRequestDTO request = new ValidarReporteRequestDTO();
        request.setAprobado(true);

        ReporteResponseDTO resultado = reporteService.validarReporte("rep-1", request, "mod1");

        assertThat(resultado.getEstado()).isEqualTo(EstadoReporte.RESUELTO);
        verify(notificacionService).notificarSistema(eq("miembro1"), any(), any());
        verify(puntosService).sumarPuntos(eq("miembro1"), any(), eq("rep-1"));
    }

    @Test
    void validarReporte_rechazado_vuelveAEnRevision() {
        Reporte reporte = reporteDeEjemplo();
        reporte.setEstado(EstadoReporte.PENDIENTE_VALIDACION);
        when(reporteRepository.findById("rep-1")).thenReturn(Optional.of(reporte));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));

        ValidarReporteRequestDTO request = new ValidarReporteRequestDTO();
        request.setAprobado(false);

        ReporteResponseDTO resultado = reporteService.validarReporte("rep-1", request, "mod1");

        assertThat(resultado.getEstado()).isEqualTo(EstadoReporte.EN_REVISION);
        assertThat(resultado.getResueltoPor()).isNull();
        verify(notificacionService, never()).notificarSistema(any(), any(), any());
    }

    @Test
    void validarReporte_noPendiente_lanzaOperacionInvalida() {
        Reporte reporte = reporteDeEjemplo();
        reporte.setEstado(EstadoReporte.RESUELTO);
        when(reporteRepository.findById("rep-1")).thenReturn(Optional.of(reporte));

        ValidarReporteRequestDTO request = new ValidarReporteRequestDTO();
        request.setAprobado(true);

        assertThatThrownBy(() -> reporteService.validarReporte("rep-1", request, "mod1"))
                .isInstanceOf(OperacionInvalidaException.class);
        verify(reporteRepository, times(1)).findById("rep-1");
    }
}