package com.photobogota.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.CambiarContrasenaDTO;
import com.photobogota.api.dto.CambiarContrasenaResponseDTO;
import com.photobogota.api.dto.EditarPerfilDTO;
import com.photobogota.api.dto.PerfilUsuarioDTO;
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

@RestController
@RequestMapping(ApiConstants.V1 + "/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Perfil público y gestión de la propia cuenta")
public class UsuarioController {

    private final IUsuarioService usuarioService;

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
}
