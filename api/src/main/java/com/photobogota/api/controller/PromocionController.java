package com.photobogota.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.CrearPromocionRequestDTO;
import com.photobogota.api.dto.PromocionResponseDTO;
import com.photobogota.api.service.PromocionService;
import com.photobogota.api.utils.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiConstants.V1 + "/promociones")
@RequiredArgsConstructor
@Tag(name = "Promociones", description = "Promociones de los locales de los socios")
public class PromocionController {

    private final PromocionService promocionService;

    @Operation(summary = "Listar promociones activas", description = "Promociones vigentes (activas y dentro de fechas) de todos los locales. Público. Lo usa el mapa para pintar el icono de promoción.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<List<PromocionResponseDTO>> listarActivas() {
        return ResponseEntity.ok(promocionService.listarActivas());
    }

    @Operation(summary = "Listar promociones del local logueado", description = "Todas las promociones (activas, programadas, expiradas o desactivadas) de los locales del socio autenticado.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/mias")
    @PreAuthorize("hasRole('SOCIO')")
    public ResponseEntity<List<PromocionResponseDTO>> listarMias(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(promocionService.listarMias(userDetails.getUsername()));
    }

    @Operation(summary = "Listar promociones de un local", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/spot/{spotId}")
    public ResponseEntity<List<PromocionResponseDTO>> listarDeSpot(@PathVariable String spotId) {
        return ResponseEntity.ok(promocionService.listarDeSpot(spotId));
    }

    @Operation(summary = "Obtener la promoción activa de un local", description = "Devuelve la promoción vigente y activa del local. 404 si no hay ninguna en este momento. Público, lo usa la página del local.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promoción activa"),
            @ApiResponse(responseCode = "404", description = "El local no tiene promoción activa")
    })
    @GetMapping("/spot/{spotId}/activa")
    public ResponseEntity<PromocionResponseDTO> obtenerActivaDeSpot(@PathVariable String spotId) {
        return ResponseEntity.ok(promocionService.obtenerActivaDeSpot(spotId));
    }

    @Operation(summary = "Obtener detalle de una promoción", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle de la promoción"),
            @ApiResponse(responseCode = "404", description = "Promoción no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PromocionResponseDTO> obtenerPromocion(@PathVariable String id) {
        return ResponseEntity.ok(promocionService.obtenerPorId(id));
    }

    @Operation(summary = "Crear una promoción", description = "Crea una promoción en un local propio del socio. Requiere rol SOCIO.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Promoción creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "El local no pertenece al socio")
    })
    @PostMapping
    @PreAuthorize("hasRole('SOCIO')")
    public ResponseEntity<PromocionResponseDTO> crearPromocion(
            @Valid @RequestBody CrearPromocionRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(promocionService.crearPromocion(request, userDetails.getUsername()));
    }

    @Operation(summary = "Actualizar una promoción", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SOCIO')")
    public ResponseEntity<PromocionResponseDTO> actualizarPromocion(
            @PathVariable String id,
            @Valid @RequestBody CrearPromocionRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(promocionService.actualizarPromocion(id, request, userDetails.getUsername()));
    }

    @Operation(summary = "Activar/desactivar una promoción", description = "Alterna la visibilidad pública de la promoción.", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('SOCIO')")
    public ResponseEntity<PromocionResponseDTO> togglePromocion(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(promocionService.desactivarPromocion(id, userDetails.getUsername()));
    }

    @Operation(summary = "Duplicar una promoción", description = "Crea una copia (desactivada, 30 días desde hoy) del local del socio.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/duplicar")
    @PreAuthorize("hasRole('SOCIO')")
    public ResponseEntity<PromocionResponseDTO> duplicarPromocion(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(promocionService.duplicarPromocion(id, userDetails.getUsername()));
    }

    @Operation(summary = "Eliminar una promoción", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SOCIO')")
    public ResponseEntity<Void> eliminarPromocion(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        promocionService.eliminarPromocion(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}