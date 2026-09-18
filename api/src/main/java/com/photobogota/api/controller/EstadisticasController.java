package com.photobogota.api.controller;

import com.photobogota.api.dto.EstadisticasSocioDTO;
import com.photobogota.api.service.EstadisticasSocioService;
import com.photobogota.api.utils.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.V1 + "/estadisticas")
@RequiredArgsConstructor
@Tag(name = "Estadísticas del Socio", description = "Métricas de visitas, reseñas, calificaciones y usos de promociones de los locales del socio")
public class EstadisticasController {

    private final EstadisticasSocioService estadisticasService;

    @Operation(summary = "Estadísticas propias del socio", description = "Retorna las métricas agregadas de los locales del socio autenticado para el período indicado.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estadísticas calculadas"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Se requiere rol SOCIO")
    })
    @GetMapping("/socio")
    @PreAuthorize("hasRole('SOCIO')")
    public ResponseEntity<EstadisticasSocioDTO> obtenerEstadisticasSocio(
            @Parameter(description = "Período a calcular: semana, mes o ano", example = "mes") @RequestParam(defaultValue = "mes") String periodo,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(estadisticasService.obtenerEstadisticas(userDetails.getUsername(), periodo));
    }
}