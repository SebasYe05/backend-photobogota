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

import com.photobogota.api.dto.CanjeResponseDTO;
import com.photobogota.api.dto.CanjearRequestDTO;
import com.photobogota.api.dto.ValidarCanjeRequestDTO;
import com.photobogota.api.service.CanjeService;
import com.photobogota.api.utils.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiConstants.V1 + "/canjes")
@RequiredArgsConstructor
@Tag(name = "Canjes", description = "Canjes de promociones entre miembros y socios")
public class CanjeController {

    private final CanjeService canjeService;

    @Operation(summary = "Canjear una promoción", description = "Un MIEMBRO canjea una promoción activa y obtiene un código de 8 caracteres. 409 si ya hay un canje vigente o usado para esa promoción.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Canje creado con su código"),
            @ApiResponse(responseCode = "400", description = "Promoción no activa o fuera de fechas"),
            @ApiResponse(responseCode = "404", description = "Promoción no encontrada"),
            @ApiResponse(responseCode = "409", description = "Promoción agotada o ya canjeada por el miembro")
    })
    @PostMapping
    @PreAuthorize("hasRole('MIEMBRO')")
    public ResponseEntity<CanjeResponseDTO> crearCanje(
            @Valid @RequestBody CanjearRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(canjeService.crearCanje(request, userDetails.getUsername()));
    }

    @Operation(summary = "Mis canjes", description = "Todos los canjes del MIEMBRO autenticado.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/mios")
    @PreAuthorize("hasRole('MIEMBRO')")
    public ResponseEntity<List<CanjeResponseDTO>> listarMios(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(canjeService.listarMios(userDetails.getUsername()));
    }

    @Operation(summary = "Canjes de una promoción", description = "Los canjes de la promoción, solo para el socio dueño de la promoción.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de canjes"),
            @ApiResponse(responseCode = "403", description = "El socio no es dueño de la promoción"),
            @ApiResponse(responseCode = "404", description = "Promoción no encontrada")
    })
    @GetMapping("/promocion/{promocionId}")
    @PreAuthorize("hasRole('SOCIO')")
    public ResponseEntity<List<CanjeResponseDTO>> listarDePromocion(
            @PathVariable String promocionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(canjeService.listarDePromocion(promocionId, userDetails.getUsername()));
    }

    @Operation(summary = "Validar un código de canje", description = "El SOCIO dueño del local valida el código presentado por el miembro y lo marca como usado.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Canje marcado como USADO"),
            @ApiResponse(responseCode = "400", description = "El código no corresponde al local o la promoción no está activa"),
            @ApiResponse(responseCode = "403", description = "El local no pertenece al socio"),
            @ApiResponse(responseCode = "404", description = "Local o código no encontrado"),
            @ApiResponse(responseCode = "409", description = "El código ya fue utilizado"),
            @ApiResponse(responseCode = "410", description = "El código está vencido")
    })
    @PostMapping("/validar")
    @PreAuthorize("hasRole('SOCIO')")
    public ResponseEntity<CanjeResponseDTO> validarCanje(
            @Valid @RequestBody ValidarCanjeRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(canjeService.validarCanje(request, userDetails.getUsername()));
    }
}