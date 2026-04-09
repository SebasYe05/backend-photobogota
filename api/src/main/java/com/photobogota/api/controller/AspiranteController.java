package com.photobogota.api.controller;

import com.photobogota.api.dto.AspiranteResponseDTO;
import com.photobogota.api.dto.SolicitudAspiranteDTO;
import com.photobogota.api.model.EstadoAspirante;
import com.photobogota.api.service.IAspiranteService;
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

    private final IAspiranteService aspiranteService;

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

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<AspiranteResponseDTO> obtenerPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(aspiranteService.obtenerPorCodigo(codigo));
    }

    @GetMapping
    public ResponseEntity<List<AspiranteResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(aspiranteService.obtenerTodos());
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<AspiranteResponseDTO>> obtenerPorEstado(@PathVariable EstadoAspirante estado) {
        return ResponseEntity.ok(aspiranteService.obtenerPorEstado(estado));
    }

    @PutMapping("/{id}/aprobar")
    public ResponseEntity<AspiranteResponseDTO> aprobarAspirante(@PathVariable String id) {
        return ResponseEntity.ok(aspiranteService.aprobarAspirante(id));
    }

    @PutMapping("/{id}/rechazar")
    public ResponseEntity<AspiranteResponseDTO> rechazarAspirante(@PathVariable String id) {
        return ResponseEntity.ok(aspiranteService.rechazarAspirante(id));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<AspiranteResponseDTO> actualizarEstado(
            @PathVariable String id, 
            @RequestParam EstadoAspirante estado) {
        return ResponseEntity.ok(aspiranteService.actualizarEstado(id, estado));
    }
}