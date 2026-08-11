package com.photobogota.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.PuntosResponseDTO;
import com.photobogota.api.service.IPuntosService;
import com.photobogota.api.utils.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiConstants.V1 + "/usuarios/me/puntos")
@RequiredArgsConstructor
@Tag(name = "Puntos", description = "Consulta de puntos y nivel del usuario autenticado")
public class PuntosController {

    private final IPuntosService puntosService;

    @Operation(summary = "Obtener mis puntos y nivel", description = "Retorna puntos acumulados, nivel, puntos de hoy y progreso hacia el siguiente nivel", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos de puntos obtenidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @GetMapping
    public ResponseEntity<PuntosResponseDTO> obtenerMisPuntos(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(puntosService.obtenerPuntos(userDetails.getUsername()));
    }
}
