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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiConstants.V1)
@RequiredArgsConstructor
@Tag(name = "Categorias y Localidades de spots", description = "Operaciones relacionadas con el catálogo de categorías y localidades")
public class CategoriaController {

    private final ICategoriaService categoriaService;
    private final ILocalidadService localidadService;

    @Operation(summary = "Listar categorías activas", description = "Retorna las categorías fotográficas activas para filtrar spots")
    @ApiResponse(responseCode = "200", description = "Lista de categorías activas")
    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaDTO>> obtenerCategoriasActivas() {
        return ResponseEntity.ok(categoriaService.obtenerActivos());
    }

    @Operation(summary = "Listar localidades activas", description = "Retorna las localidades de Bogotá activas para filtrar spots")
    @ApiResponse(responseCode = "200", description = "Lista de localidades activas")
    @GetMapping("/localidades")
    public ResponseEntity<List<LocalidadDTO>> obtenerLocalidadessActivas() {
        return ResponseEntity.ok(localidadService.obtenerActivos());
    }
}