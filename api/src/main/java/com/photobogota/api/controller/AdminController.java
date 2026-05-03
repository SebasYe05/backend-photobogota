package com.photobogota.api.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.photobogota.api.config.RateLimitService;
import com.photobogota.api.dto.CrearUsuarioRequestDTO;
import com.photobogota.api.dto.RegistroResponseDTO;
import com.photobogota.api.dto.UsuarioListResponseDTO;
import com.photobogota.api.service.IAdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Administración", description = "Gestión de usuarios y monitoreo del sistema — requiere rol ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final IAdminService adminService;
    private final RateLimitService rateLimitService;

    @Operation(summary = "Crear usuario", description = "Crea un nuevo usuario con el rol especificado (ADMIN, MOD, SOCIO, MIEMBRO)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o usuario ya existente"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    @PostMapping("/usuarios")
    public ResponseEntity<RegistroResponseDTO> crearUsuario(@Valid @RequestBody CrearUsuarioRequestDTO dto) {
        RegistroResponseDTO nuevoUsuario = adminService.crearUsuario(dto);
        return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
    }

    @Operation(summary = "Listar usuarios", description = "Retorna todos los usuarios paginados")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuarios"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    @GetMapping("/usuarios")
    public ResponseEntity<Page<UsuarioListResponseDTO>> listarUsuarios(
            @Parameter(description = "Número de página (inicia en 0)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de registros por página", example = "10") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UsuarioListResponseDTO> usuarios = adminService.listarUsuarios(pageable);
        return ResponseEntity.ok(usuarios);
    }

    @Operation(summary = "Activar o desactivar usuario", description = "Cambia el estado de la cuenta de un usuario (activo/inactivo)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    @PatchMapping("/usuarios/{id}/estado")
    public ResponseEntity<Void> actualizarEstadoUsuario(
            @Parameter(description = "ID del usuario", required = true) @PathVariable String id,
            @Parameter(description = "true para activar, false para desactivar", required = true) @RequestParam boolean activo) {
        adminService.actualizarEstadoUsuario(id, activo);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Eliminar usuario", description = "Elimina permanentemente un usuario del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario eliminado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> eliminarUsuario(
            @Parameter(description = "ID del usuario a eliminar", required = true) @PathVariable String id) {
        adminService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Estadísticas de rate limiting", description = "Retorna métricas actuales del limitador de tasa de peticiones")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estadísticas del rate limiter"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    @GetMapping("/rate-limit/stats")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticasRateLimit() {
        return ResponseEntity.ok(rateLimitService.obtenerEstadisticas());
    }
}