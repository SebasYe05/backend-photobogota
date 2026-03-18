package com.photobogota.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.RegistroAdminRequestDTO;
import com.photobogota.api.dto.RegistroResponseDTO;
import com.photobogota.api.service.IAdminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {
 
    private final IAdminService adminService;

    @PostMapping("/crear-admin")
    public ResponseEntity<RegistroResponseDTO> crearAdmin(@Valid @RequestBody RegistroAdminRequestDTO dto) {
        RegistroResponseDTO nuevoAdmin = adminService.registrarAdmin(dto);
        return new ResponseEntity<>(nuevoAdmin, HttpStatus.CREATED);
    }
}
