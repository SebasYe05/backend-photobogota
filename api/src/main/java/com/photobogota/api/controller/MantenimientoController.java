package com.photobogota.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.EstadoMantenimientoDTO;
import com.photobogota.api.dto.MantenimientoResponseDTO;
import com.photobogota.api.dto.ProgramarMantenimientoRequestDTO;
import com.photobogota.api.service.IMantenimientoService;
import com.photobogota.api.utils.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Mantenimiento", description = "Programación de ventanas de mantenimiento del sistema (HU #47)")
public class MantenimientoController {

    private final IMantenimientoService mantenimientoService;

    @Operation(summary = "Consultar estado de mantenimiento", description = "Indica si el sistema está actualmente en mantenimiento. Usado por el front para mostrar el aviso al usuario.")
    @ApiResponse(responseCode = "200", description = "Estado actual de mantenimiento")
    @GetMapping(ApiConstants.V1 + "/mantenimiento/estado")
    public ResponseEntity<EstadoMantenimientoDTO> obtenerEstado() {
        return ResponseEntity.ok(mantenimientoService.obtenerEstado());
    }

    @Operation(summary = "Listar mantenimientos programados", description = "Devuelve los mantenimientos activos/futuros no cancelados, para la pantalla de administración.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Listado de mantenimientos programados")
    @GetMapping(ApiConstants.V1 + "/admin/mantenimiento")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MantenimientoResponseDTO>> listarProgramados() {
        return ResponseEntity.ok(mantenimientoService.listarProgramados());
    }

    @Operation(summary = "Programar mantenimiento", description = "Programa una ventana de mantenimiento y notifica automáticamente a todos los usuarios.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mantenimiento programado y aviso enviado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (ej. fecha fin antes que fecha inicio)"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    @PostMapping(ApiConstants.V1 + "/admin/mantenimiento")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MantenimientoResponseDTO> programar(
            @Valid @RequestBody ProgramarMantenimientoRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        MantenimientoResponseDTO creado = mantenimientoService.programar(request, userDetails.getUsername());
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @Operation(summary = "Cancelar mantenimiento", description = "Cancela una ventana de mantenimiento programada y avisa a todos los usuarios.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mantenimiento cancelado"),
            @ApiResponse(responseCode = "404", description = "Mantenimiento no encontrado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    @DeleteMapping(ApiConstants.V1 + "/admin/mantenimiento/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelar(@PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        mantenimientoService.cancelar(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // Validaciones de negocio (ej. fechas inválidas, cancelar dos veces) devuelven 400 en vez de 500.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleDatosInvalidos(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
