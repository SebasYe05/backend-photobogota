package com.photobogota.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.EditarPerfilDTO;
import com.photobogota.api.dto.PerfilUsuarioDTO;
import com.photobogota.api.service.IUsuarioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class UsuarioController {
 
    private final IUsuarioService usuarioService;
    
    @PutMapping("/perfil/{nombreUsuario}")
    public ResponseEntity<PerfilUsuarioDTO> editarPerfil(
            @PathVariable String nombreUsuario,
            @RequestBody EditarPerfilDTO dto) {
        PerfilUsuarioDTO perfilActualizado = usuarioService.editarPerfil(nombreUsuario, dto);
        return ResponseEntity.ok(perfilActualizado);
    }
    
    @GetMapping("/perfil/{nombreUsuario}")
    public ResponseEntity<PerfilUsuarioDTO> obtenerPerfil(@PathVariable String nombreUsuario) {
        PerfilUsuarioDTO perfil = usuarioService.obtenerPerfil(nombreUsuario);
        return ResponseEntity.ok(perfil);
    }
}
