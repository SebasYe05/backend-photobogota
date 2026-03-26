package com.photobogota.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.CambiarContrasenaDTO;
import com.photobogota.api.dto.CambiarContrasenaResponseDTO;
import com.photobogota.api.dto.EditarPerfilDTO;
import com.photobogota.api.dto.PerfilUsuarioDTO;
import com.photobogota.api.service.IUsuarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class UsuarioController {

    private final IUsuarioService usuarioService;

    @GetMapping("/perfil/{nombreUsuario}")
    public ResponseEntity<PerfilUsuarioDTO> obtenerPerfil(@PathVariable String nombreUsuario) {
        return ResponseEntity.ok(usuarioService.obtenerPerfil(nombreUsuario));
    }

    @PutMapping("/perfil")
    public ResponseEntity<PerfilUsuarioDTO> editarPerfil(
            @AuthenticationPrincipal UserDetails currentUser, 
            @Valid @RequestBody EditarPerfilDTO dto) {

        // El Service usa el nombre que viene en el token cifrado, no el de la URL
        PerfilUsuarioDTO perfilActualizado = usuarioService.editarPerfil(currentUser.getUsername(), dto);
        return ResponseEntity.ok(perfilActualizado);
    }

    @PatchMapping("/me/password")
    public ResponseEntity<CambiarContrasenaResponseDTO> cambiarContrasena(
            // Extraemos el usuario directamente del contexto de seguridad (JWT)
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody CambiarContrasenaDTO dto) {

        // Usamos el username del token en lugar de la PathVariable
        CambiarContrasenaResponseDTO response = usuarioService.cambiarContrasena(currentUser.getUsername(), dto);

        return ResponseEntity.ok(response);
    }
}
