package com.photobogota.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.CategoriaDTO;
import com.photobogota.api.dto.LocalidadDTO;
import com.photobogota.api.service.ICategoriaService;
import com.photobogota.api.service.ILocalidadService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/moderador")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ModeradorController {

    private final ICategoriaService categoriaService;
    private final ILocalidadService localidadService;

    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaDTO>> obtenerCategorias() {
        return ResponseEntity.ok(categoriaService.obtenerTodos());
    }

    @GetMapping("/categorias/activas")
    public ResponseEntity<List<CategoriaDTO>> obtenerCategoriasActivas() {
        return ResponseEntity.ok(categoriaService.obtenerActivos());
    }

    @GetMapping("/categorias/{id}")
    public ResponseEntity<CategoriaDTO> obtenerCategoriaPorId(@PathVariable String id) {
        return ResponseEntity.ok(categoriaService.obtenerPorId(id));
    }

    @PostMapping("/categorias")
    public ResponseEntity<CategoriaDTO> crearCategoria(@RequestBody CategoriaDTO dto) {
        CategoriaDTO nueva = categoriaService.crear(dto);
        return new ResponseEntity<>(nueva, HttpStatus.CREATED);
    }

    @PutMapping("/categorias/{id}")
    public ResponseEntity<CategoriaDTO> actualizarCategoria(@PathVariable String id, @RequestBody CategoriaDTO dto) {
        return ResponseEntity.ok(categoriaService.actualizar(id, dto));
    }

    @DeleteMapping("/categorias/{id}")
    public ResponseEntity<Void> eliminarCategoria(@PathVariable String id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/categorias/{id}/toggle")
    public ResponseEntity<CategoriaDTO> toggleCategoria(@PathVariable String id) {
        return ResponseEntity.ok(categoriaService.togglestatus(id));
    }

    @GetMapping("/localidades")
    public ResponseEntity<List<LocalidadDTO>> obtenerLocalidadess() {
        return ResponseEntity.ok(localidadService.obtenerTodos());
    }

    @GetMapping("/localidades/activas")
    public ResponseEntity<List<LocalidadDTO>> obtenerLocalidadessActivas() {
        return ResponseEntity.ok(localidadService.obtenerActivos());
    }

    @GetMapping("/localidades/{id}")
    public ResponseEntity<LocalidadDTO> obtenerLocalidadPorId(@PathVariable String id) {
        return ResponseEntity.ok(localidadService.obtenerPorId(id));
    }

    @PostMapping("/localidades")
    public ResponseEntity<LocalidadDTO> crearLocalidad(@RequestBody LocalidadDTO dto) {
        LocalidadDTO nueva = localidadService.crear(dto);
        return new ResponseEntity<>(nueva, HttpStatus.CREATED);
    }

    @PutMapping("/localidades/{id}")
    public ResponseEntity<LocalidadDTO> actualizarLocalidad(@PathVariable String id, @RequestBody LocalidadDTO dto) {
        return ResponseEntity.ok(localidadService.actualizar(id, dto));
    }

    @DeleteMapping("/localidades/{id}")
    public ResponseEntity<Void> eliminarLocalidad(@PathVariable String id) {
        localidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/localidades/{id}/toggle")
    public ResponseEntity<LocalidadDTO> toggleLocalidad(@PathVariable String id) {
        return ResponseEntity.ok(localidadService.toggleStatus(id));
    }
}