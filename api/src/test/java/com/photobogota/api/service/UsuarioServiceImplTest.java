package com.photobogota.api.service;

import com.photobogota.api.dto.EditarPerfilDTO;
import com.photobogota.api.dto.PerfilUsuarioDTO;
import com.photobogota.api.model.Miembro;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.Usuario;
import com.photobogota.api.model.UsuarioAuth;
import com.photobogota.api.repository.UsuarioAuthRepository;
import com.photobogota.api.repository.UsuarioRepository;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// ============================================================
//  @ExtendWith(MockitoExtension.class)
//  ─────────────────────────────────
//  Le indica a JUnit 5 que use la extensión de Mockito.
//  Esto permite que las anotaciones @Mock e @InjectMocks
//  sean procesadas automáticamente antes de cada test,
//  sin necesidad de llamar MockitoAnnotations.openMocks(this)
//  manualmente en un método @BeforeEach.
// ============================================================
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

        // ============================================================
        // MOCKS — Dependencias simuladas
        // ============================================================
        // @Mock crea un objeto "fantasma" que reemplaza a la clase real.
        // Por defecto, todos sus métodos devuelven null / 0 / false.
        // Nosotros definimos su comportamiento con when(...).thenReturn(...)
        // solo para los casos que necesita cada test (principio de
        // "stubbing mínimo necesario").
        // ============================================================

        @Mock
        private UsuarioRepository usuarioRepository; // Simula la BD de usuarios

        @Mock
        private UsuarioAuthRepository usuarioAuthRepository; // Simula la BD de autenticación

        @Mock
        private PasswordEncoder passwordEncoder; // Simula el encriptador de contraseñas

        // ============================================================
        // @InjectMocks — La clase bajo prueba (Subject Under Test)
        // ============================================================
        // Mockito instancia UsuarioServiceImpl e inyecta automáticamente
        // los tres @Mock declarados arriba como si fuera Spring,
        // pero SIN levantar el contexto de Spring (más rápido).
        // ============================================================
        @InjectMocks
        private UsuarioServiceImpl usuarioService;

        // ============================================================
        // CONVENCIÓN DE NOMBRES: método_Estado_ResultadoEsperado
        // ─────────────────────────────────────────────────────
        // Hace que el reporte de tests sea autoexplicativo.
        // Si el test falla, el nombre ya indica qué salió mal.
        // ============================================================

        // ────────────────────────────────────────────────────────────
        // TEST 1: Camino feliz (Happy Path)
        // Verifica que cuando el usuario existe, el perfil se actualiza
        // correctamente y se retorna el DTO con los nuevos valores.
        // ────────────────────────────────────────────────────────────
        @Test
        void editarPerfil_CuandoUsuarioExiste_DebeActualizarYRetornarPerfilDTO() {

                // --------------------------------------------------------
                // FASE 1 — ARRANGE (Preparar)
                // Se construye todo el escenario previo a la ejecución:
                // datos de entrada, entidades simuladas y comportamiento
                // de los mocks.
                // --------------------------------------------------------

                String nombreUsuario = "bogotano123";
                ObjectId userId = new ObjectId();

                // DTO de entrada: simula el JSON que llega desde el cliente
                EditarPerfilDTO dtoEdicion = new EditarPerfilDTO();
                dtoEdicion.setNombresCompletos("Juan Pérez Editado");
                dtoEdicion.setBiografia("Nueva biografía sobre Bogotá");
                dtoEdicion.setFotoPerfil("http://imagenes.com/nueva.jpg");

                // Entidad de autenticación: simula la fila en la tabla usuario_auth
                UsuarioAuth usuarioAuthSimulado = new UsuarioAuth();
                usuarioAuthSimulado.setId(userId);
                usuarioAuthSimulado.setNombreUsuario(nombreUsuario);
                usuarioAuthSimulado.setEmail("juan@photobogota.com");
                usuarioAuthSimulado.setRol(Rol.MIEMBRO);

                Miembro miembroSimulado = Miembro.builder()
                                .id(userId)
                                .nombresCompletos("Juan Pérez Original")
                                .biografia("Biografía vieja")
                                .fotoPerfil("http://imagenes.com/vieja.jpg")
                                .puntos(100L)
                                .nivel(1)
                                .build();

                // STUBBING de Mocks
                // Definimos qué devuelven los repositorios cuando el servicio
                // los llame con argumentos específicos.

                // Cuando el servicio busque por nombre de usuario -> devolver la entidad
                // simulada
                when(usuarioAuthRepository.findByNombreUsuario(nombreUsuario))
                                .thenReturn(Optional.of(usuarioAuthSimulado));

                // Cuando el servicio busque el perfil por ID, devolver el miembro simulado
                when(usuarioRepository.findById(usuarioAuthSimulado.getId()))
                                .thenReturn(Optional.of(miembroSimulado));

                // Cuando el servicio guarde CUALQUIER Usuario ->> devolverlo (simula el .save()
                // de JPA)
                // any(Usuario.class) acepta cualquier instancia de Usuario, sin importar su
                // estado.
                when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

                // --------------------------------------------------------
                // FASE 2 — ACT (Ejecutar)
                // Se invoca el método que queremos probar con los datos
                // preparados en ARRANGE. Solo UNA línea de ejecución.
                // --------------------------------------------------------
                PerfilUsuarioDTO resultado = usuarioService.editarPerfil(nombreUsuario, dtoEdicion);

                // --------------------------------------------------------
                // FASE 3 — ASSERT (Verificar)
                // Se comprueba que el resultado cumpla exactamente lo esperado.
                // AssertJ permite encadenar aserciones de forma fluida y legible.
                // --------------------------------------------------------

                // El resultado no debe ser nulo
                assertThat(resultado).isNotNull();

                // Los campos deben reflejar los valores del DTO de entrada (ya editados)
                assertThat(resultado.getNombresCompletos()).isEqualTo("Juan Pérez Editado");
                assertThat(resultado.getBiografia()).isEqualTo("Nueva biografía sobre Bogotá");
                assertThat(resultado.getFotoPerfil()).isEqualTo("http://imagenes.com/nueva.jpg");

                // El nombre de usuario debe conservarse sin cambios
                assertThat(resultado.getNombreUsuario()).isEqualTo(nombreUsuario);

                // VERIFICACIÓN de interacciones (Mockito.verify)
                // Confirma que el repositorio fue llamado exactamente 1 vez con .save().
                // Si el servicio llama save() dos veces (bug), este test lo detecta.
                verify(usuarioRepository, times(1)).save(any(Usuario.class));
        }

        // ────────────────────────────────────────────────────────────
        // TEST 2: Camino de error (Sad Path / Edge Case)
        // Verifica que cuando el usuario NO existe, el servicio lanza
        // la excepción correcta y NO intenta operar sobre la BD.
        // ────────────────────────────────────────────────────────────
        @Test
        void editarPerfil_CuandoUsuarioNoExiste_DebeLanzarExcepcion() {

                // --------------------------------------------------------
                // FASE 1 — ARRANGE
                // --------------------------------------------------------

                String nombreUsuarioInexistente = "usuario_fantasma";
                EditarPerfilDTO dto = new EditarPerfilDTO(); // DTO vacío; no importa su contenido

                // Simulamos que el repositorio NO encuentra al usuario - >S devuelve Optional
                // vacío
                when(usuarioAuthRepository.findByNombreUsuario(nombreUsuarioInexistente))
                                .thenReturn(Optional.empty());

                // --------------------------------------------------------
                // FASE 2 y 3 — ACT & ASSERT combinados
                // assertThatThrownBy ejecuta el código y captura la excepción
                // que se lanza. Luego verificamos su tipo con isInstanceOf().
                // --------------------------------------------------------
                assertThatThrownBy(() -> usuarioService.editarPerfil(nombreUsuarioInexistente, dto))
                                .isInstanceOf(RuntimeException.class);

                // ── VERIFICACIONES de NO-EJECUCIÓN ─────────────────────
                // Si el usuario no existe, el servicio NO debe llegar a buscar
                // en usuarioRepository ni a guardar nada.
                // Esto confirma que el flujo se cortó correctamente en el primer if.

                // never() = este método jamás debió ser invocado
                verify(usuarioRepository, never()).findById(any());
                verify(usuarioRepository, never()).save(any());
        }
}

// ============================================================
// RESUMEN DE BUENAS PRÁCTICAS APLICADAS
// ============================================================
//
// 1. Patrón AAA (Arrange-Act-Assert):
// Cada test está dividido en tres secciones claras y separadas.
// Mejora la legibilidad y el mantenimiento.
//
// 2. Nombres descriptivos de tests:
// Formato: método_Estado_ResultadoEsperado
// El nombre del test explica el escenario sin leer el cuerpo.
//
// 3. Un test = una responsabilidad:
// Cada test verifica un único comportamiento. Si hay N escenarios,
// hay N métodos @Test independientes.
//
// 4. Mocks solo para dependencias externas:
// Solo se mockean repositorios y servicios externos (BD, APIs).
// La lógica del servicio bajo prueba se ejecuta REAL.
//
// 5. Stubbing mínimo (solo lo necesario):
// Se define el comportamiento del mock solo para lo que ese test
// específico necesita. Evita acoplar tests entre sí.
//
// 6. verify() para efectos secundarios:
// Cuando el método no retorna un valor o su efecto es una
// escritura en BD, verify() confirma que ocurrió (o no ocurrió).
//
// 7. Happy Path + Sad Path:
// Se prueban tanto el flujo exitoso como los casos de error.
// Los tests de error son tan importantes como los de éxito.
// ============================================================