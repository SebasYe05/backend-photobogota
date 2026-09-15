package com.photobogota.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.photobogota.api.dto.PuntosResponseDTO;
import com.photobogota.api.model.HistorialPuntos;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.PuntosConfig;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.TipoPuntos;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.HistorialPuntosRepository;
import com.photobogota.api.repository.MiembroRepository;
import com.photobogota.api.repository.PuntosConfigRepository;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PuntosServiceImplTest {

    @Mock
    private MiembroRepository miembroRepository;

    @Mock
    private UsuarioAuthRepository usuarioAuthRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private HistorialPuntosRepository historialPuntosRepository;

    @Mock
    private PuntosConfigRepository puntosConfigRepository;

    @Mock
    private INotificacionService notificacionService;

    @InjectMocks
    private PuntosServiceImpl puntosService;

    @Test
    void sumarPuntos_miembroNuevo_nivelUnoPuntosCero() {
        String username = "nuevo_miembro";
        ObjectId userId = new ObjectId();
        UsuarioAuth auth = UsuarioAuth.builder()
                .id(userId)
                .nombreUsuario(username)
                .rol(Rol.MIEMBRO)
                .build();
        Miembro miembro = Miembro.builder()
                .id(userId)
                .puntos(0L)
                .nivel(1)
                .build();

        when(usuarioAuthRepository.findByNombreUsuario(username)).thenReturn(Optional.of(auth));
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(miembro));
        when(historialPuntosRepository.existsByUsuarioAndTipoAndRefId(anyString(), any(), anyString()))
                .thenReturn(false);
        when(puntosConfigRepository.findById(anyString())).thenReturn(Optional.empty());
        when(miembroRepository.save(any(Miembro.class))).thenAnswer(inv -> inv.getArgument(0));

        int otorgados = puntosService.sumarPuntos(username, TipoPuntos.CREAR_SPOT, "spot-1");

        assertThat(otorgados).isEqualTo(10);
        assertThat(miembro.getPuntos()).isEqualTo(10);
        assertThat(miembro.getNivel()).isEqualTo(1);
    }

    @Test
    void sumarPuntos_crearSpot_daDiezPuntos() {
        String username = "fotografo";
        ObjectId userId = new ObjectId();
        UsuarioAuth auth = UsuarioAuth.builder()
                .id(userId)
                .nombreUsuario(username)
                .rol(Rol.MIEMBRO)
                .build();
        Miembro miembro = Miembro.builder()
                .id(userId)
                .puntos(0L)
                .nivel(1)
                .build();

        when(usuarioAuthRepository.findByNombreUsuario(username)).thenReturn(Optional.of(auth));
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(miembro));
        when(historialPuntosRepository.existsByUsuarioAndTipoAndRefId(anyString(), any(), anyString()))
                .thenReturn(false);
        when(puntosConfigRepository.findById(anyString())).thenReturn(Optional.empty());
        when(miembroRepository.save(any(Miembro.class))).thenAnswer(inv -> inv.getArgument(0));

        int otorgados = puntosService.sumarPuntos(username, TipoPuntos.CREAR_SPOT, "spot-1");

        assertThat(otorgados).isEqualTo(10);
        assertThat(miembro.getPuntos()).isEqualTo(10);
        verify(miembroRepository).save(miembro);
    }

    @Test
    void sumarPuntos_noSuperaLimiteDiario() {
        String username = "fotografo";
        ObjectId userId = new ObjectId();
        UsuarioAuth auth = UsuarioAuth.builder()
                .id(userId)
                .nombreUsuario(username)
                .rol(Rol.MIEMBRO)
                .build();
        Miembro miembro = Miembro.builder()
                .id(userId)
                .puntos(95L)
                .nivel(1)
                .build();

        when(usuarioAuthRepository.findByNombreUsuario(username)).thenReturn(Optional.of(auth));
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(miembro));
        when(historialPuntosRepository.existsByUsuarioAndTipoAndRefId(anyString(), any(), anyString()))
                .thenReturn(false);
        when(puntosConfigRepository.findById(anyString())).thenReturn(Optional.empty());
        when(historialPuntosRepository.findByUsuarioAndFechaGreaterThanEqual(anyString(), any(LocalDateTime.class)))
                .thenReturn(List.of(HistorialPuntos.builder().puntos(95).build()));
        when(miembroRepository.save(any(Miembro.class))).thenAnswer(inv -> inv.getArgument(0));

        int otorgados = puntosService.sumarPuntos(username, TipoPuntos.CREAR_SPOT, "spot-2");

        assertThat(otorgados).isEqualTo(5);
        assertThat(miembro.getPuntos()).isEqualTo(100);
    }

    @Test
    void sumarPuntos_segundaCalificacionMismoSpot_noDuplica() {
        String username = "fotografo";
        ObjectId userId = new ObjectId();
        UsuarioAuth auth = UsuarioAuth.builder()
                .id(userId)
                .nombreUsuario(username)
                .rol(Rol.MIEMBRO)
                .build();
        Miembro miembro = Miembro.builder()
                .id(userId)
                .puntos(0L)
                .nivel(1)
                .build();

        when(usuarioAuthRepository.findByNombreUsuario(username)).thenReturn(Optional.of(auth));
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(miembro));
        when(historialPuntosRepository.existsByUsuarioAndTipoAndRefId(username, TipoPuntos.CALIFICAR_SPOT, "cal-1"))
                .thenReturn(true);

        int otorgados = puntosService.sumarPuntos(username, TipoPuntos.CALIFICAR_SPOT, "cal-1");

        assertThat(otorgados).isEqualTo(0);
        assertThat(miembro.getPuntos()).isEqualTo(0);
        verify(miembroRepository, never()).save(any());
    }

    @Test
    void sumarPuntos_reporteValidado_unaSolaVez() {
        String username = "fotografo";
        ObjectId userId = new ObjectId();
        UsuarioAuth auth = UsuarioAuth.builder()
                .id(userId)
                .nombreUsuario(username)
                .rol(Rol.MIEMBRO)
                .build();
        Miembro miembro = Miembro.builder()
                .id(userId)
                .puntos(0L)
                .nivel(1)
                .build();

        when(usuarioAuthRepository.findByNombreUsuario(username)).thenReturn(Optional.of(auth));
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(miembro));
        when(historialPuntosRepository.existsByUsuarioAndTipoAndRefId(username, TipoPuntos.REPORTE_VALIDADO, "rep-1"))
                .thenReturn(true);

        int otorgados = puntosService.sumarPuntos(username, TipoPuntos.REPORTE_VALIDADO, "rep-1");

        assertThat(otorgados).isEqualTo(0);
        verify(miembroRepository, never()).save(any());
    }

    @Test
    void sumarPuntos_alSubirNivel_disparaNotificacion() {
        String username = "fotografo";
        ObjectId userId = new ObjectId();
        UsuarioAuth auth = UsuarioAuth.builder()
                .id(userId)
                .nombreUsuario(username)
                .rol(Rol.MIEMBRO)
                .build();
        Miembro miembro = Miembro.builder()
                .id(userId)
                .puntos(90L)
                .nivel(1)
                .build();

        when(usuarioAuthRepository.findByNombreUsuario(username)).thenReturn(Optional.of(auth));
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(miembro));
        when(historialPuntosRepository.existsByUsuarioAndTipoAndRefId(anyString(), any(), anyString()))
                .thenReturn(false);
        when(puntosConfigRepository.findById(anyString())).thenReturn(Optional.empty());
        when(miembroRepository.save(any(Miembro.class))).thenAnswer(inv -> inv.getArgument(0));

        int otorgados = puntosService.sumarPuntos(username, TipoPuntos.CREAR_SPOT, "spot-3");

        assertThat(otorgados).isEqualTo(10);
        assertThat(miembro.getPuntos()).isEqualTo(100);
        assertThat(miembro.getNivel()).isEqualTo(2);
        verify(notificacionService).notificarSubidaNivel(username, 2);
    }

    @Test
    void actualizarConfig_actualizaValorYAfectaProximoAward() {
        when(puntosConfigRepository.findById("puntos.crear_spot"))
                .thenReturn(Optional.of(PuntosConfig.builder().clave("puntos.crear_spot").valor("20").build()));
        when(puntosConfigRepository.findAll()).thenReturn(List.of(
                PuntosConfig.builder().clave("puntos.crear_spot").valor("20").build()
        ));

        Map<String, String> resultado = puntosService.actualizarConfig(Map.of("puntos.crear_spot", "20"));

        assertThat(resultado).containsEntry("puntos.crear_spot", "20");

        String username = "fotografo";
        ObjectId userId = new ObjectId();
        UsuarioAuth auth = UsuarioAuth.builder().id(userId).nombreUsuario(username).rol(Rol.MIEMBRO).build();
        Miembro miembro = Miembro.builder().id(userId).puntos(0L).nivel(1).build();

        when(usuarioAuthRepository.findByNombreUsuario(username)).thenReturn(Optional.of(auth));
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(miembro));
        when(historialPuntosRepository.existsByUsuarioAndTipoAndRefId(anyString(), any(), anyString()))
                .thenReturn(false);
        when(puntosConfigRepository.findById("puntos.crear_spot"))
                .thenReturn(Optional.of(PuntosConfig.builder().clave("puntos.crear_spot").valor("20").build()));
        when(miembroRepository.save(any(Miembro.class))).thenAnswer(inv -> inv.getArgument(0));

        int otorgados = puntosService.sumarPuntos(username, TipoPuntos.CREAR_SPOT, "spot-4");
        assertThat(otorgados).isEqualTo(20);
        assertThat(miembro.getPuntos()).isEqualTo(20);
    }

    @Test
    void ajustarPuntosPorId_deltaPositivo_recalculaNivel() {
        String usuarioId = new ObjectId().toHexString();
        ObjectId userId = new ObjectId(usuarioId);
        String username = "admin_ajuste";
        UsuarioAuth auth = UsuarioAuth.builder().id(userId).nombreUsuario(username).rol(Rol.MIEMBRO).build();
        Miembro miembro = Miembro.builder().id(userId).puntos(0L).nivel(1).build();

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(miembro));
        when(usuarioAuthRepository.findById(userId)).thenReturn(Optional.of(auth));
        when(puntosConfigRepository.findById(anyString())).thenReturn(Optional.empty());
        when(miembroRepository.save(any(Miembro.class))).thenAnswer(inv -> inv.getArgument(0));

        PuntosResponseDTO respuesta = puntosService.ajustarPuntosPorId(usuarioId, 110, "Bonus");

        assertThat(respuesta).isNotNull();
        assertThat(miembro.getPuntos()).isEqualTo(110);
        assertThat(miembro.getNivel()).isEqualTo(2);
        verify(notificacionService).notificarSubidaNivel(username, 2);
    }

    @Test
    void ajustarPuntosPorId_deltaNegativo_noBajaDeCero() {
        String usuarioId = new ObjectId().toHexString();
        ObjectId userId = new ObjectId(usuarioId);
        String username = "admin_ajuste";
        UsuarioAuth auth = UsuarioAuth.builder().id(userId).nombreUsuario(username).rol(Rol.MIEMBRO).build();
        Miembro miembro = Miembro.builder().id(userId).puntos(5L).nivel(1).build();

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(miembro));
        when(usuarioAuthRepository.findById(userId)).thenReturn(Optional.of(auth));
        when(puntosConfigRepository.findById(anyString())).thenReturn(Optional.empty());
        when(miembroRepository.save(any(Miembro.class))).thenAnswer(inv -> inv.getArgument(0));

        PuntosResponseDTO respuesta = puntosService.ajustarPuntosPorId(usuarioId, -10, "Corrección");

        assertThat(respuesta).isNotNull();
        assertThat(miembro.getPuntos()).isEqualTo(0);
        assertThat(miembro.getNivel()).isEqualTo(1);
    }

    // --- obtenerPuntos: sin cobertura previa. El bug reportado por QA
    // (progresoPercent siempre 0 en nivel 1) no se detectaba porque no había
    // ningún test para este método. ---

    @Test
    void obtenerPuntos_nivelUnoConPuntos_progresoPercentCoherenteConElTotal() {
        String username = "fotografo";
        ObjectId userId = new ObjectId();
        UsuarioAuth auth = UsuarioAuth.builder().id(userId).nombreUsuario(username).rol(Rol.MIEMBRO).build();
        Miembro miembro = Miembro.builder().id(userId).puntos(10L).nivel(1).build();

        when(usuarioAuthRepository.findByNombreUsuario(username)).thenReturn(Optional.of(auth));
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(miembro));
        when(puntosConfigRepository.findById(anyString())).thenReturn(Optional.empty());
        when(historialPuntosRepository.findByUsuarioAndFechaGreaterThanEqual(anyString(), any(LocalDateTime.class)))
                .thenReturn(List.of(HistorialPuntos.builder().puntos(10).build()));

        PuntosResponseDTO respuesta = puntosService.obtenerPuntos(username);

        // Caso exacto reportado por QA: 10 puntos en nivel 1 (base de nivel 2 = 100)
        assertThat(respuesta.getPuntos()).isEqualTo(10);
        assertThat(respuesta.getNivel()).isEqualTo(1);
        assertThat(respuesta.getPuntosHoy()).isEqualTo(10);
        assertThat(respuesta.getPuntosParaSiguienteNivel()).isEqualTo(90);
        assertThat(respuesta.getProgresoPercent()).isEqualTo(10);
    }

    @Test
    void obtenerPuntos_miembroSinPuntos_progresoPercentCero() {
        String username = "nuevo_miembro";
        ObjectId userId = new ObjectId();
        UsuarioAuth auth = UsuarioAuth.builder().id(userId).nombreUsuario(username).rol(Rol.MIEMBRO).build();
        Miembro miembro = Miembro.builder().id(userId).puntos(0L).nivel(1).build();

        when(usuarioAuthRepository.findByNombreUsuario(username)).thenReturn(Optional.of(auth));
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(miembro));
        when(puntosConfigRepository.findById(anyString())).thenReturn(Optional.empty());
        when(historialPuntosRepository.findByUsuarioAndFechaGreaterThanEqual(anyString(), any(LocalDateTime.class)))
                .thenReturn(List.of());

        PuntosResponseDTO respuesta = puntosService.obtenerPuntos(username);

        assertThat(respuesta.getPuntos()).isEqualTo(0);
        assertThat(respuesta.getProgresoPercent()).isEqualTo(0);
        assertThat(respuesta.getPuntosParaSiguienteNivel()).isEqualTo(100);
    }

    @Test
    void obtenerPuntos_nivelDos_progresoPercentEnElTramoCorrecto() {
        String username = "fotografo_pro";
        ObjectId userId = new ObjectId();
        UsuarioAuth auth = UsuarioAuth.builder().id(userId).nombreUsuario(username).rol(Rol.MIEMBRO).build();
        // Umbral nivel 2 = 100, umbral nivel 3 = floor(100 * 1.5) = 150
        Miembro miembro = Miembro.builder().id(userId).puntos(125L).nivel(2).build();

        when(usuarioAuthRepository.findByNombreUsuario(username)).thenReturn(Optional.of(auth));
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(miembro));
        when(puntosConfigRepository.findById(anyString())).thenReturn(Optional.empty());
        when(historialPuntosRepository.findByUsuarioAndFechaGreaterThanEqual(anyString(), any(LocalDateTime.class)))
                .thenReturn(List.of());

        PuntosResponseDTO respuesta = puntosService.obtenerPuntos(username);

        assertThat(respuesta.getNivel()).isEqualTo(2);
        assertThat(respuesta.getPuntosParaSiguienteNivel()).isEqualTo(25);
        assertThat(respuesta.getProgresoPercent()).isEqualTo(50);
    }

    @Test
    void obtenerPuntos_usuarioNoMiembro_respuestaVaciaSinRomper() {
        String username = "moderador_1";
        UsuarioAuth auth = UsuarioAuth.builder().nombreUsuario(username).rol(Rol.MOD).build();

        when(usuarioAuthRepository.findByNombreUsuario(username)).thenReturn(Optional.of(auth));
        when(puntosConfigRepository.findById(anyString())).thenReturn(Optional.empty());

        PuntosResponseDTO respuesta = puntosService.obtenerPuntos(username);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getPuntos()).isEqualTo(0);
        assertThat(respuesta.getNivel()).isEqualTo(1);
        assertThat(respuesta.getProgresoPercent()).isEqualTo(0);
    }
}
