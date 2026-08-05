package com.photobogota.api.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.photobogota.api.dto.MetricasEliminacionDTO;
import com.photobogota.api.dto.ProcesarEliminacionAdminDTO;
import com.photobogota.api.dto.SolicitudEliminacionAdminDTO;
import com.photobogota.api.model.EstadoSolicitudEliminacion;
import com.photobogota.api.service.IAdminEliminacionCuentaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

/**
 * Panel de administración para procesar solicitudes de eliminación de
 * cuenta: verificación automática de identidad, gestión de dependencias
 * (spots y reportes pendientes), anonimización inmediata, notificaciones a
 * las partes afectadas y métricas agregadas.
 *
 * Todas las rutas ya quedan protegidas por SecurityConfig bajo
 * "/api/v1/admin/**" -> hasRole("ADMIN").
 */
@RestController
@RequestMapping("/api/v1/admin/eliminaciones")
@RequiredArgsConstructor
@Tag(name = "Administración - Eliminación de cuentas", description = "Procesamiento de solicitudes de eliminación de cuenta — requiere rol ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class AdminEliminacionController {

    private final IAdminEliminacionCuentaService adminEliminacionCuentaService;

    @Operation(summary = "Listar solicitudes de eliminación", description = "Lista paginada de solicitudes, opcionalmente filtrada por estado. Incluye verificación automática de identidad y dependencias detectadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de solicitudes"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    @GetMapping
    public ResponseEntity<Page<SolicitudEliminacionAdminDTO>> listarSolicitudes(
            @Parameter(description = "Filtrar por estado: PENDIENTE_VERIFICACION, PROGRAMADA, CANCELADA o COMPLETADA") @RequestParam(required = false) EstadoSolicitudEliminacion estado,
            @Parameter(description = "Número de página (inicia en 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de registros por página") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminEliminacionCuentaService.listarSolicitudes(estado, pageable));
    }

    @Operation(summary = "Detalle de una solicitud", description = "Devuelve el detalle completo de una solicitud, con la verificación automática de identidad y las dependencias pendientes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle de la solicitud"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    @GetMapping("/{id}")
    public ResponseEntity<SolicitudEliminacionAdminDTO> obtenerDetalle(@PathVariable String id) {
        return ResponseEntity.ok(adminEliminacionCuentaService.obtenerDetalle(id));
    }

    @Operation(summary = "Procesar eliminación de inmediato", description = "Verifica identidad, resuelve dependencias (spots/reportes), anonimiza los datos personales de inmediato (sin esperar los 30 días) y notifica a las partes afectadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta anonimizada y solicitud completada"),
            @ApiResponse(responseCode = "400", description = "La solicitud ya fue procesada o no se pudo verificar la identidad"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    @PostMapping("/{id}/procesar")
    public ResponseEntity<String> procesarInmediatamente(
            @PathVariable String id,
            @RequestBody(required = false) ProcesarEliminacionAdminDTO dto,
            @AuthenticationPrincipal UserDetails currentUser) {
        String mensaje = adminEliminacionCuentaService.procesarInmediatamente(id, currentUser.getUsername(), dto);
        return ResponseEntity.ok(mensaje);
    }

    @Operation(summary = "Rechazar solicitud de eliminación", description = "Cancela una solicitud activa (por ejemplo, ante una disputa) y reactiva la cuenta del usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud rechazada y cuenta reactivada"),
            @ApiResponse(responseCode = "400", description = "La solicitud no está en un estado que se pueda rechazar"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<String> rechazarSolicitud(
            @PathVariable String id,
            @RequestBody(required = false) ProcesarEliminacionAdminDTO dto,
            @AuthenticationPrincipal UserDetails currentUser) {
        String mensaje = adminEliminacionCuentaService.rechazarSolicitud(id, currentUser.getUsername(), dto);
        return ResponseEntity.ok(mensaje);
    }

    @Operation(summary = "Métricas de eliminaciones", description = "Estadísticas agregadas de las solicitudes de eliminación: por estado, motivo, rol y tiempos de procesamiento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Métricas agregadas"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    @GetMapping("/metricas")
    public ResponseEntity<MetricasEliminacionDTO> obtenerMetricas() {
        return ResponseEntity.ok(adminEliminacionCuentaService.obtenerMetricas());
    }
}
