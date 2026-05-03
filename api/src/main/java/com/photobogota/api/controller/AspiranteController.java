package com.photobogota.api.controller;

import com.photobogota.api.dto.AspiranteResponseDTO;
import com.photobogota.api.dto.SolicitudAspiranteDTO;
import com.photobogota.api.model.EstadoAspirante;
import com.photobogota.api.service.IAspiranteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/aspirantes")
@RequiredArgsConstructor
@Tag(name = "Aspirantes", description = "Gestión de solicitudes de aspirantes a socios")
public class AspiranteController {

    private final IAspiranteService aspiranteService;

    @Operation(summary = "Enviar solicitud de ingreso", description = "Crea una nueva solicitud para unirse a PhotoBogotá como fotógrafo. No requiere cuenta previa.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitud creada, pendiente de revisión"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o email ya tiene solicitud activa")
    })
    @PostMapping
    public ResponseEntity<AspiranteResponseDTO> crearSolicitud(
            @Valid @RequestBody SolicitudAspiranteDTO request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(aspiranteService.crearSolicitud(request));
    }

    @Operation(summary = "Obtener solicitud por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud encontrada"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AspiranteResponseDTO> obtenerPorId(
            @Parameter(description = "ID de la solicitud", required = true) @PathVariable String id) {
        return ResponseEntity.ok(aspiranteService.obtenerPorId(id));
    }

    @Operation(summary = "Obtener solicitud por email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud encontrada"),
            @ApiResponse(responseCode = "404", description = "No existe solicitud con ese email")
    })
    @GetMapping("/email/{email}")
    public ResponseEntity<AspiranteResponseDTO> obtenerPorEmail(
            @Parameter(description = "Email del aspirante", example = "juan.romero@example.com") @PathVariable String email) {
        return ResponseEntity.ok(aspiranteService.obtenerPorEmail(email));
    }

    @Operation(summary = "Obtener solicitud por código de seguimiento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud encontrada"),
            @ApiResponse(responseCode = "404", description = "Código no encontrado")
    })
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<AspiranteResponseDTO> obtenerPorCodigo(
            @Parameter(description = "Código único de seguimiento de la solicitud") @PathVariable String codigo) {
        return ResponseEntity.ok(aspiranteService.obtenerPorCodigo(codigo));
    }

    @Operation(summary = "Listar todas las solicitudes", description = "Retorna todas las solicitudes de aspirantes sin filtrar por estado")
    @ApiResponse(responseCode = "200", description = "Lista de solicitudes")
    @GetMapping
    public ResponseEntity<List<AspiranteResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(aspiranteService.obtenerTodos());
    }

    @Operation(summary = "Filtrar solicitudes por estado", description = "Retorna solicitudes según su estado: PENDIENTE, APROBADO o RECHAZADO")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada por estado"),
            @ApiResponse(responseCode = "400", description = "Estado no válido")
    })
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<AspiranteResponseDTO>> obtenerPorEstado(
            @Parameter(description = "Estado de la solicitud", example = "pendiente") @PathVariable EstadoAspirante estado) {
        return ResponseEntity.ok(aspiranteService.obtenerPorEstado(estado));
    }

    @Operation(summary = "Aprobar solicitud", description = "Aprueba un aspirante y genera su cuenta de usuario. Requiere rol ADMIN o MOD.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aspirante aprobado, cuenta creada"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos suficientes")
    })
    @PutMapping("/{id}/aprobar")
    public ResponseEntity<AspiranteResponseDTO> aprobarAspirante(
            @Parameter(description = "ID de la solicitud a aprobar", required = true) @PathVariable String id) {
        return ResponseEntity.ok(aspiranteService.aprobarAspirante(id));
    }

    @Operation(summary = "Rechazar solicitud", description = "Rechaza la solicitud de un aspirante. Requiere rol ADMIN o MOD.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud rechazada"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos suficientes")
    })
    @PutMapping("/{id}/rechazar")
    public ResponseEntity<AspiranteResponseDTO> rechazarAspirante(
            @Parameter(description = "ID de la solicitud a rechazar", required = true) @PathVariable String id) {
        return ResponseEntity.ok(aspiranteService.rechazarAspirante(id));
    }

    @Operation(summary = "Actualizar estado manualmente", description = "Permite cambiar el estado de una solicitud a cualquier valor. Requiere rol ADMIN.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    })
    @PutMapping("/{id}/estado")
    public ResponseEntity<AspiranteResponseDTO> actualizarEstado(
            @Parameter(description = "ID de la solicitud", required = true) @PathVariable String id,
            @Parameter(description = "Nuevo estado", example = "aprobado") @RequestParam EstadoAspirante estado) {
        return ResponseEntity.ok(aspiranteService.actualizarEstado(id, estado));
    }
}