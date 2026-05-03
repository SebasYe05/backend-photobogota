package com.photobogota.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.CategoriaDTO;
import com.photobogota.api.dto.LocalidadDTO;
import com.photobogota.api.service.ICategoriaService;
import com.photobogota.api.service.ILocalidadService;
import com.photobogota.api.utils.ApiConstants;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiConstants.V1)
@RequiredArgsConstructor
public class CategoriaController {

    private final ICategoriaService categoriaService;
    private final ILocalidadService localidadService;

    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaDTO>> obtenerCategoriasActivas() {
        return ResponseEntity.ok(categoriaService.obtenerActivos());
    }

    @GetMapping("/localidades")
    public ResponseEntity<List<LocalidadDTO>> obtenerLocalidadessActivas() {
        return ResponseEntity.ok(localidadService.obtenerActivos());
    }
}