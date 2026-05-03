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

import java.util.List;

@RestController
@RequestMapping(ApiConstants.V1 + "/spots")
@RequiredArgsConstructor
public class SpotController {

    private final SpotService spotService;

    @GetMapping
    public ResponseEntity<List<SpotResumenDTO>> obtenerSpots(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String localidad) {

        return ResponseEntity.ok(spotService.obtenerTodos(categoria, localidad));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpotResponseDTO> obtenerSpot(@PathVariable String id) {
        return ResponseEntity.ok(spotService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MIEMBRO', 'SOCIO', 'MODERADOR')")
    public ResponseEntity<SpotResponseDTO> crearSpot(
            @Valid @RequestBody CrearSpotRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(spotService.crearSpot(request, userDetails.getUsername()));
    }

    @PostMapping("/{id}/resenas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SpotResponseDTO> agregarResena(
            @PathVariable String id,
            @Valid @RequestBody ResenaRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(spotService.agregarResena(id, request, userDetails.getUsername()));
    }
}