// AdminController.java
package com.photobogota.api.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.photobogota.api.config.RateLimitService;
import com.photobogota.api.dto.CrearUsuarioRequestDTO;
import com.photobogota.api.dto.RegistroResponseDTO;
import com.photobogota.api.dto.UsuarioListResponseDTO;
import com.photobogota.api.service.IAdminService;
import com.photobogota.api.utils.ApiConstants;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiConstants.V1 + "/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final IAdminService adminService;
    private final RateLimitService rateLimitService;

    @PostMapping("/usuarios")
    public ResponseEntity<RegistroResponseDTO> crearUsuario(@Valid @RequestBody CrearUsuarioRequestDTO dto) {
        RegistroResponseDTO nuevoUsuario = adminService.crearUsuario(dto);
        return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
    }

    @GetMapping("/usuarios")
    public ResponseEntity<Page<UsuarioListResponseDTO>> listarUsuarios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UsuarioListResponseDTO> usuarios = adminService.listarUsuarios(pageable);
        return ResponseEntity.ok(usuarios);
    }

    @PatchMapping("/usuarios/{id}/estado")
    public ResponseEntity<Void> actualizarEstadoUsuario(
            @PathVariable String id,
            @RequestParam boolean activo) {
        adminService.actualizarEstadoUsuario(id, activo);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable String id) {
        adminService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rate-limit/stats")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticasRateLimit() {
        return ResponseEntity.ok(rateLimitService.obtenerEstadisticas());
    }
}