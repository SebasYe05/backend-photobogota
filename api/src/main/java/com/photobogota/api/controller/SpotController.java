package com.photobogota.api.controller;

import com.photobogota.api.dto.*;
import com.photobogota.api.service.SpotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.photobogota.api.utils.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.V1 + "/spots")
@RequiredArgsConstructor
@Tag(name = "Spots Fotográficos", description = "Consulta y publicación de lugares para fotografiar en Bogotá")
public class SpotController {

    private final SpotService spotService;

    @Operation(summary = "Listar spots", description = "Retorna todos los spots, con filtros opcionales por categoría y/o localidad")
    @ApiResponse(responseCode = "200", description = "Lista de spots")
    @GetMapping
    public ResponseEntity<List<SpotResumenDTO>> obtenerSpots(
            @Parameter(description = "Nombre de la categoría para filtrar", example = "Paisaje urbano") @RequestParam(required = false) String categoria,
            @Parameter(description = "Nombre de la localidad para filtrar", example = "Chapinero") @RequestParam(required = false) String localidad) {
        return ResponseEntity.ok(spotService.obtenerTodos(categoria, localidad));
    }

    @Operation(summary = "Obtener detalle de un spot")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle completo del spot con reseñas"),
            @ApiResponse(responseCode = "404", description = "Spot no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<SpotResponseDTO> obtenerSpot(
            @Parameter(description = "ID del spot", required = true) @PathVariable String id) {
        return ResponseEntity.ok(spotService.obtenerPorId(id));
    }

    @Operation(summary = "Publicar nuevo spot", description = "Crea un spot fotográfico. Requiere rol MIEMBRO, SOCIO o MODERADOR.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Spot publicado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('MIEMBRO', 'SOCIO', 'MODERADOR')")
    public ResponseEntity<SpotResponseDTO> crearSpot(
            @Valid @RequestBody CrearSpotRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(spotService.crearSpot(request, userDetails.getUsername()));
    }

    @Operation(summary = "Agregar reseña a un spot", description = "Publica una calificación y comentario sobre un spot. Requiere estar autenticado.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reseña agregada, retorna spot actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "404", description = "Spot no encontrado")
    })
    @PostMapping("/{id}/resenas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SpotResponseDTO> agregarResena(
            @Parameter(description = "ID del spot a reseñar", required = true) @PathVariable String id,
            @Valid @RequestBody ResenaRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(spotService.agregarResena(id, request, userDetails.getUsername()));
    }
}