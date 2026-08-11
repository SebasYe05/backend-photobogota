package com.photobogota.api.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.AjustePuntosRequestDTO;
import com.photobogota.api.dto.PuntosResponseDTO;
import com.photobogota.api.service.IPuntosService;
import com.photobogota.api.utils.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiConstants.V1 + "/admin/puntos")
@RequiredArgsConstructor
@Tag(name = "Admin Puntos", description = "Configuración y ajustes de puntos del sistema — requiere rol ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class AdminPuntosController {

    private final IPuntosService puntosService;

    @Operation(summary = "Obtener configuración de puntos", description = "Retorna los valores actuales de puntos por acción y límites")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuración obtenida"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> obtenerConfig() {
        return ResponseEntity.ok(puntosService.obtenerConfig());
    }

    @Operation(summary = "Actualizar configuración de puntos", description = "Actualiza valores de puntos por acción, límite diario, base de umbral o timezone")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuración actualizada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    @PutMapping("/config")
    public ResponseEntity<Map<String, String>> actualizarConfig(@RequestBody Map<String, String> config) {
        return ResponseEntity.ok(puntosService.actualizarConfig(config));
    }

    @Operation(summary = "Ajustar puntos de un usuario", description = "Suma o resta puntos a un usuario MIEMBRO y recalcula su nivel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Puntos ajustados"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado o no es MIEMBRO")
    })
    @PostMapping("/usuarios/{id}/puntos")
    public ResponseEntity<PuntosResponseDTO> ajustarPuntos(
            @Parameter(description = "ID del usuario", required = true) @PathVariable String id,
            @Valid @RequestBody AjustePuntosRequestDTO request) {
        return ResponseEntity.ok(puntosService.ajustarPuntosPorId(id, request.getDelta(), request.getMotivo()));
    }
}
