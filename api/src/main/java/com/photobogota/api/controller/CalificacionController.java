package com.photobogota.api.controller;

import com.photobogota.api.dto.CalificacionRequestDTO;
import com.photobogota.api.dto.CalificacionResponseDTO;
import com.photobogota.api.service.CalificacionService;
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
@RequestMapping(ApiConstants.V1 + "/spots/{spotId}/calificaciones")
@RequiredArgsConstructor
@Tag(name = "Calificaciones de spots", description = "Calificacion de spots con estrellas")
public class CalificacionController {

    private final CalificacionService calificacionService;

    @Operation(summary = "Crear una calificacion para un spot", description = "Registra una calificacion con estrellas. Requiere autenticacion.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Calificacion creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "404", description = "Spot no encontrado")
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CalificacionResponseDTO> crearCalificacion(
            @Parameter(description = "ID del spot a calificar", required = true) @PathVariable String spotId,
            @Valid @RequestBody CalificacionRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String usuario = userDetails.getUsername();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(calificacionService.crearCalificacion(spotId, request, usuario));
    }

    @Operation(summary = "Listar calificaciones de un spot")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de calificaciones del spot"),
            @ApiResponse(responseCode = "404", description = "Spot no encontrado")
    })
    @GetMapping
    public ResponseEntity<List<CalificacionResponseDTO>> listarCalificaciones(
            @Parameter(description = "ID del spot", required = true) @PathVariable String spotId) {
        return ResponseEntity.ok(calificacionService.listarPorSpot(spotId));
    }

    @Operation(summary = "Obtener detalle de una calificacion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle de la calificacion"),
            @ApiResponse(responseCode = "404", description = "Calificacion no encontrada")
    })
    @GetMapping("/{calificacionId}")
    public ResponseEntity<CalificacionResponseDTO> obtenerCalificacion(
            @Parameter(description = "ID de la calificacion", required = true) @PathVariable String calificacionId) {
        return ResponseEntity.ok(calificacionService.obtenerPorId(calificacionId));
    }
}
