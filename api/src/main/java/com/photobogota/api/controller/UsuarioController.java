package com.photobogota.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.RegistroRequestDTO;
import com.photobogota.api.dto.RegistroResponseDTO;
import com.photobogota.api.service.IUsuarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class UsuarioController {
 
    private final IUsuarioService usuarioService;

    @PostMapping("/registro-usuario")
    public ResponseEntity<RegistroResponseDTO> registrar(@Valid @RequestBody RegistroRequestDTO dto) {
        // Al usar @Valid, Spring revisa las anotaciones (@Email, @NotBlank) del DTO
        RegistroResponseDTO nuevoUsuario = usuarioService.registrar(dto);
        return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
    }
}
