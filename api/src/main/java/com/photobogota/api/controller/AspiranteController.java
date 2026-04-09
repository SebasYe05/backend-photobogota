package com.photobogota.api.controller;

import com.photobogota.api.dto.AspiranteResponseDTO;
import com.photobogota.api.dto.SolicitudAspiranteDTO;
import com.photobogota.api.service.AspiranteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/aspirantes")
@RequiredArgsConstructor
public class AspiranteController {

    private final AspiranteService aspiranteService;

    @PostMapping
    public ResponseEntity<AspiranteResponseDTO> crearSolicitud(
            @Valid @RequestBody SolicitudAspiranteDTO request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(aspiranteService.crearSolicitud(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AspiranteResponseDTO> obtenerPorId(@PathVariable String id) {
        return ResponseEntity.ok(aspiranteService.obtenerPorId(id));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<AspiranteResponseDTO> obtenerPorEmail(@PathVariable String email) {
        return ResponseEntity.ok(aspiranteService.obtenerPorEmail(email));
    }

    @GetMapping
    public ResponseEntity<List<AspiranteResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(aspiranteService.obtenerTodos());
    }
}