package com.photobogota.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.CalificacionResponseDTO;
import com.photobogota.api.dto.CambiarContrasenaDTO;
import com.photobogota.api.dto.CambiarContrasenaResponseDTO;
import com.photobogota.api.dto.ConfirmarEliminacionDTO;
import com.photobogota.api.dto.EditarPerfilDTO;
import com.photobogota.api.dto.EstadoEliminacionDTO;
import com.photobogota.api.dto.PerfilUsuarioDTO;
import com.photobogota.api.dto.SpotResumenDTO;
import com.photobogota.api.dto.SolicitarEliminacionDTO;
import com.photobogota.api.service.IEliminacionCuentaService;
import com.photobogota.api.service.IUsuarioService;
import com.photobogota.api.utils.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiConstants.V1 + "/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Perfil público y gestión de la propia cuenta")
public class UsuarioController {

    private final IUsuarioService usuarioService;
    private final IEliminacionCuentaService eliminacionCuentaService;

    @Operation(summary = "Ver perfil público de un usuario", description = "Retorna la información pública del perfil por nombre de usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping("/perfil/{nombreUsuario}")
    public ResponseEntity<PerfilUsuarioDTO> obtenerPerfil(
            @Parameter(description = "Nombre de usuario", example = "fotografo_bogota") @PathVariable String nombreUsuario) {
        return ResponseEntity.ok(usuarioService.obtenerPerfil(nombreUsuario));
    }

    @Operation(summary = "Editar mi perfil", description = "Actualiza los datos del perfil del usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @PutMapping("/perfil")
    public ResponseEntity<PerfilUsuarioDTO> editarPerfil(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody EditarPerfilDTO dto) {
        PerfilUsuarioDTO perfilActualizado = usuarioService.editarPerfil(currentUser.getUsername(), dto);
        return ResponseEntity.ok(perfilActualizado);
    }

    @Operation(summary = "Cambiar contraseña", description = "Cambia la contraseña del usuario autenticado. Requiere ingresar la contraseña actual.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña actualizada"),
            @ApiResponse(responseCode = "400", description = "Contraseña actual incorrecta o confirmación no coincide"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @PatchMapping("/me/password")
    public ResponseEntity<CambiarContrasenaResponseDTO> cambiarContrasena(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody CambiarContrasenaDTO dto) {
        CambiarContrasenaResponseDTO response = usuarioService.cambiarContrasena(currentUser.getUsername(), dto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Mis spots", description = "Retorna los spots creados por el usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de spots del usuario")
    })
    @GetMapping("/me/spots")
    public ResponseEntity<List<SpotResumenDTO> > obtenerMisSpots(
            @AuthenticationPrincipal UserDetails currentUser) {
        List<SpotResumenDTO> spots = usuarioService.obtenerSpotsDeUsuario(currentUser.getUsername());
        return ResponseEntity.ok(spots != null ? spots : Collections.emptyList());
    }

    @Operation(summary = "Mis reseñas", description = "Retorna las calificaciones (reseñas) escritas por el usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de reseñas del usuario")
    })
    @GetMapping("/me/resenas")
    public ResponseEntity<List<CalificacionResponseDTO>> obtenerMisResenas(
            @AuthenticationPrincipal UserDetails currentUser) {
        List<CalificacionResponseDTO> resenas = usuarioService.obtenerResenasDeUsuario(currentUser.getUsername());
        return ResponseEntity.ok(resenas != null ? resenas : Collections.emptyList());
    }

    @Operation(summary = "Mis guardados", description = "Retorna la lista de spots guardados por el usuario autenticado (mismo shape que GET /spots)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de spots guardados")
    })
    @GetMapping("/me/guardados")
    public ResponseEntity<List<SpotResumenDTO>> obtenerMisGuardados(
            @AuthenticationPrincipal UserDetails currentUser) {
        List<SpotResumenDTO> guardados = usuarioService.obtenerGuardados(currentUser.getUsername());
        return ResponseEntity.ok(guardados != null ? guardados : Collections.emptyList());
    }

    @Operation(summary = "Guardar un spot", description = "Guarda un spot en la lista de favoritos del usuario autenticado (idempotente)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Spot guardado (o ya estaba guardado)"),
            @ApiResponse(responseCode = "404", description = "Spot no encontrado")
    })
    @PostMapping("/me/guardados/{spotId}")
    public ResponseEntity<SpotResumenDTO> guardarSpot(
            @AuthenticationPrincipal UserDetails currentUser,
            @PathVariable String spotId) {
        SpotResumenDTO spot = usuarioService.guardarSpot(currentUser.getUsername(), spotId);
        return ResponseEntity.ok(spot);
    }

    @Operation(summary = "Quitar un spot guardado", description = "Elimina un spot de la lista de favoritos del usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Guardado eliminado (o no existía)")
    })
    @DeleteMapping("/me/guardados/{spotId}")
    public ResponseEntity<Void> quitarGuardado(
            @AuthenticationPrincipal UserDetails currentUser,
            @PathVariable String spotId) {
        usuarioService.quitarGuardado(currentUser.getUsername(), spotId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Spots de un usuario", description = "Retorna los spots públicos creados por un usuario específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de spots del usuario"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping("/{nombreUsuario}/spots")
    public ResponseEntity<List<SpotResumenDTO>> obtenerSpotsDeUsuario(
            @Parameter(description = "Nombre de usuario", example = "fotografo_bogota") @PathVariable String nombreUsuario) {
        List<SpotResumenDTO> spots = usuarioService.obtenerSpotsDeUsuario(nombreUsuario);
        return ResponseEntity.ok(spots != null ? spots : Collections.emptyList());
    }

    @Operation(summary = "Reseñas de un usuario", description = "Retorna las calificaciones (reseñas) escritas por un usuario específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de reseñas del usuario"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping("/{nombreUsuario}/resenas")
    public ResponseEntity<List<CalificacionResponseDTO>> obtenerResenasDeUsuario(
            @Parameter(description = "Nombre de usuario", example = "fotografo_bogota") @PathVariable String nombreUsuario) {
        List<CalificacionResponseDTO> resenas = usuarioService.obtenerResenasDeUsuario(nombreUsuario);
        return ResponseEntity.ok(resenas != null ? resenas : Collections.emptyList());
    }

    @Operation(summary = "Solicitar eliminación de mi cuenta", description = "Envía un código de verificación por correo para confirmar la eliminación. Solo disponible para MIEMBRO.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código de verificación enviado al correo"),
            @ApiResponse(responseCode = "400", description = "Ya existe una solicitud en curso"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Solo disponible para MIEMBRO")
    })
    @PostMapping("/me/eliminacion/solicitar")
    public ResponseEntity<Map<String, String>> solicitarEliminacionCuenta(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody(required = false) SolicitarEliminacionDTO dto) {
        SolicitarEliminacionDTO body = dto != null ? dto : SolicitarEliminacionDTO.builder().build();
        String mensaje = eliminacionCuentaService.solicitarEliminacion(currentUser.getUsername(), body);
        return ResponseEntity.ok(Map.of("mensaje", mensaje));
    }

    @Operation(summary = "Confirmar eliminación de mi cuenta", description = "Valida el código recibido por correo, desactiva la cuenta y da 30 días para recuperarla.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eliminación confirmada y programada"),
            @ApiResponse(responseCode = "400", description = "Código inválido, expirado o ya usado"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @PostMapping("/me/eliminacion/confirmar")
    public ResponseEntity<Map<String, String>> confirmarEliminacionCuenta(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody ConfirmarEliminacionDTO dto) {
        String mensaje = eliminacionCuentaService.confirmarEliminacion(currentUser.getUsername(), dto);
        return ResponseEntity.ok(Map.of("mensaje", mensaje));
    }

    @Operation(summary = "Cancelar eliminación de mi cuenta", description = "Recupera la cuenta dentro del período de 30 días.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eliminación cancelada, cuenta reactivada"),
            @ApiResponse(responseCode = "400", description = "No hay solicitud activa o el plazo ya venció"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @PostMapping("/me/eliminacion/cancelar")
    public ResponseEntity<Map<String, String>> cancelarEliminacionCuenta(
            @AuthenticationPrincipal UserDetails currentUser) {
        String mensaje = eliminacionCuentaService.cancelarEliminacion(currentUser.getUsername());
        return ResponseEntity.ok(Map.of("mensaje", mensaje));
    }

    @Operation(summary = "Estado de mi solicitud de eliminación", description = "Indica si el usuario tiene una solicitud de eliminación activa y sus detalles.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de la solicitud"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @GetMapping("/me/eliminacion/estado")
    public ResponseEntity<EstadoEliminacionDTO> obtenerEstadoEliminacionCuenta(
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(eliminacionCuentaService.obtenerEstado(currentUser.getUsername()));
    }
}
