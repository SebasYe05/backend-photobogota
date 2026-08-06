package com.photobogota.api.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.ApelacionRequestDTO;
import com.photobogota.api.dto.SancionDTO;
import com.photobogota.api.service.IFiltroContenidoService;
import com.photobogota.api.utils.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiConstants.V1 + "/moderacion")
@RequiredArgsConstructor
@Tag(name = "Moderación (usuario)", description = "Consulta de la propia sanción y apelación de bans")
@SecurityRequirement(name = "bearerAuth")
public class ModeracionController {

    private final IFiltroContenidoService filtroContenidoService;

    @Operation(summary = "Mi sanción actual", description = "Devuelve la sanción activa del usuario autenticado (o null si no tiene)")
    @GetMapping("/mi-sancion")
    public ResponseEntity<SancionDTO> obtenerMiSancion(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(filtroContenidoService.obtenerSancionActual(userDetails.getUsername()));
    }

    @Operation(summary = "Apelar suspensión indefinida", description = "Envía una apelación cuando el usuario tiene un ban activo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Apelación enviada"),
            @ApiResponse(responseCode = "400", description = "No hay ban activo o ya existe una apelación pendiente")
    })
    @PostMapping("/mi-sancion/apelar")
    public ResponseEntity<Map<String, String>> apelarBan(
            @Valid @RequestBody ApelacionRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        filtroContenidoService.apelarBan(userDetails.getUsername(), request.getMotivo());
        return ResponseEntity.ok(Map.of("mensaje", "Apelación enviada correctamente. Un administrador la revisará."));
    }
}
