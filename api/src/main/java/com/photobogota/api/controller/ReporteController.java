package com.photobogota.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.CrearReporteRequestDTO;
import com.photobogota.api.dto.ReporteResponseDTO;
import com.photobogota.api.model.Rol;
import com.photobogota.api.service.IReporteService;
import com.photobogota.api.utils.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiConstants.V1 + "/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Sistema de reportes de la comunidad (Etapa 1: creación y asignación automática)")
public class ReporteController {

    private final IReporteService reporteService;

    @Operation(summary = "Crear un reporte", description = "Crea un reporte, genera un número de ticket y lo asigna automáticamente a ADMIN o MOD según la categoría.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reporte creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReporteResponseDTO> crearReporte(
            @Valid @RequestBody CrearReporteRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String usuario = userDetails.getUsername();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reporteService.crearReporte(request, usuario));
    }

    @Operation(summary = "Ver mis reportes", description = "Lista los reportes creados por el usuario autenticado.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/mios")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReporteResponseDTO>> listarMisReportes(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reporteService.listarMisReportes(userDetails.getUsername()));
    }

    @Operation(summary = "Obtener detalle de un reporte", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle del reporte"),
            @ApiResponse(responseCode = "404", description = "Reporte no encontrado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReporteResponseDTO> obtenerReporte(@PathVariable String id) {
        return ResponseEntity.ok(reporteService.obtenerPorId(id));
    }

    @Operation(summary = "Listar reportes asignados a moderadores", description = "Cola de reportes de categoría ofensivo, spam, información incorrecta y problema con spot.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/asignados/moderador")
    @PreAuthorize("hasAnyRole('MOD', 'ADMIN')")
    public ResponseEntity<List<ReporteResponseDTO>> listarAsignadosAModerador() {
        return ResponseEntity.ok(reporteService.listarPorRolAsignado(Rol.MOD));
    }

    @Operation(summary = "Listar reportes asignados a administradores", description = "Cola de reportes de categoría error técnico.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/asignados/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReporteResponseDTO>> listarAsignadosAAdmin() {
        return ResponseEntity.ok(reporteService.listarPorRolAsignado(Rol.ADMIN));
    }
}
