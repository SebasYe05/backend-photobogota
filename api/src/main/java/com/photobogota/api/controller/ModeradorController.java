package com.photobogota.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.photobogota.api.utils.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiConstants.V1 + "/moderador")
@RequiredArgsConstructor
@Tag(name = "Moderación", description = "Gestión de categorías y localidades — requiere rol MOD")
@SecurityRequirement(name = "bearerAuth")
public class ModeradorController {

    private final ICategoriaService categoriaService;
    private final ILocalidadService localidadService;

    // ---- CATEGORÍAS ----

    @Operation(summary = "Listar todas las categorías (incluye inactivas)")
    @ApiResponse(responseCode = "200", description = "Lista completa de categorías")
    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaDTO>> obtenerCategorias() {
        return ResponseEntity.ok(categoriaService.obtenerTodos());
    }

    @Operation(summary = "Listar categorías activas")
    @ApiResponse(responseCode = "200", description = "Lista de categorías activas")
    @GetMapping("/categorias/activas")
    public ResponseEntity<List<CategoriaDTO>> obtenerCategoriasActivas() {
        return ResponseEntity.ok(categoriaService.obtenerActivos());
    }

    @Operation(summary = "Obtener categoría por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría encontrada"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @GetMapping("/categorias/{id}")
    public ResponseEntity<CategoriaDTO> obtenerCategoriaPorId(
            @Parameter(description = "ID de la categoría", required = true) @PathVariable String id) {
        return ResponseEntity.ok(categoriaService.obtenerPorId(id));
    }

    @Operation(summary = "Crear categoría")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoría creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping("/categorias")
    public ResponseEntity<CategoriaDTO> crearCategoria(@RequestBody CategoriaDTO dto) {
        CategoriaDTO nueva = categoriaService.crear(dto);
        return new ResponseEntity<>(nueva, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar categoría")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría actualizada"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @PutMapping("/categorias/{id}")
    public ResponseEntity<CategoriaDTO> actualizarCategoria(
            @Parameter(description = "ID de la categoría", required = true) @PathVariable String id,
            @RequestBody CategoriaDTO dto) {
        return ResponseEntity.ok(categoriaService.actualizar(id, dto));
    }

    @Operation(summary = "Eliminar categoría")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoría eliminada"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @DeleteMapping("/categorias/{id}")
    public ResponseEntity<Void> eliminarCategoria(
            @Parameter(description = "ID de la categoría", required = true) @PathVariable String id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Activar / desactivar categoría", description = "Cambia el estado activo/inactivo de una categoría")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado cambiado"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @PatchMapping("/categorias/{id}/toggle")
    public ResponseEntity<CategoriaDTO> toggleCategoria(
            @Parameter(description = "ID de la categoría", required = true) @PathVariable String id) {
        return ResponseEntity.ok(categoriaService.togglestatus(id));
    }

    // ---- LOCALIDADES ----

    @Operation(summary = "Listar todas las localidades (incluye inactivas)")
    @ApiResponse(responseCode = "200", description = "Lista completa de localidades")
    @GetMapping("/localidades")
    public ResponseEntity<List<LocalidadDTO>> obtenerLocalidadess() {
        return ResponseEntity.ok(localidadService.obtenerTodos());
    }

    @Operation(summary = "Listar localidades activas")
    @ApiResponse(responseCode = "200", description = "Lista de localidades activas")
    @GetMapping("/localidades/activas")
    public ResponseEntity<List<LocalidadDTO>> obtenerLocalidadessActivas() {
        return ResponseEntity.ok(localidadService.obtenerActivos());
    }

    @Operation(summary = "Obtener localidad por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Localidad encontrada"),
            @ApiResponse(responseCode = "404", description = "Localidad no encontrada")
    })
    @GetMapping("/localidades/{id}")
    public ResponseEntity<LocalidadDTO> obtenerLocalidadPorId(
            @Parameter(description = "ID de la localidad", required = true) @PathVariable String id) {
        return ResponseEntity.ok(localidadService.obtenerPorId(id));
    }

    @Operation(summary = "Crear localidad")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Localidad creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping("/localidades")
    public ResponseEntity<LocalidadDTO> crearLocalidad(@RequestBody LocalidadDTO dto) {
        LocalidadDTO nueva = localidadService.crear(dto);
        return new ResponseEntity<>(nueva, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar localidad")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Localidad actualizada"),
            @ApiResponse(responseCode = "404", description = "Localidad no encontrada")
    })
    @PutMapping("/localidades/{id}")
    public ResponseEntity<LocalidadDTO> actualizarLocalidad(
            @Parameter(description = "ID de la localidad", required = true) @PathVariable String id,
            @RequestBody LocalidadDTO dto) {
        return ResponseEntity.ok(localidadService.actualizar(id, dto));
    }

    @Operation(summary = "Eliminar localidad")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Localidad eliminada"),
            @ApiResponse(responseCode = "404", description = "Localidad no encontrada")
    })
    @DeleteMapping("/localidades/{id}")
    public ResponseEntity<Void> eliminarLocalidad(
            @Parameter(description = "ID de la localidad", required = true) @PathVariable String id) {
        localidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Activar / desactivar localidad", description = "Cambia el estado activo/inactivo de una localidad")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado cambiado"),
            @ApiResponse(responseCode = "404", description = "Localidad no encontrada")
    })
    @PatchMapping("/localidades/{id}/toggle")
    public ResponseEntity<LocalidadDTO> toggleLocalidad(
            @Parameter(description = "ID de la localidad", required = true) @PathVariable String id) {
        return ResponseEntity.ok(localidadService.toggleStatus(id));
    }
}