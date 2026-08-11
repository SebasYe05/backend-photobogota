package com.photobogota.api.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.PalabraProhibidaDTO;
import com.photobogota.api.dto.RegistroModeracionDTO;
import com.photobogota.api.dto.ResolverApelacionRequestDTO;
import com.photobogota.api.model.AccionModeracion;
import com.photobogota.api.model.TipoContenidoModerado;
import com.photobogota.api.service.IFiltroContenidoService;
import com.photobogota.api.utils.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiConstants.V1 + "/admin/moderacion")
@RequiredArgsConstructor
@Tag(name = "Filtro de contenido (Admin)", description = "Gestión del filtro automático de contenido: palabras prohibidas, historial de moderación y apelaciones — requiere rol ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class AdminModeracionController {

    private final IFiltroContenidoService filtroContenidoService;

    // ---- Palabras y frases prohibidas ----

    @Operation(summary = "Listar palabras y frases prohibidas")
    @GetMapping("/palabras")
    public ResponseEntity<List<PalabraProhibidaDTO>> listarPalabras() {
        return ResponseEntity.ok(filtroContenidoService.listarPalabras());
    }

    @Operation(summary = "Crear palabra o frase prohibida")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Regla creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping("/palabras")
    public ResponseEntity<PalabraProhibidaDTO> crearPalabra(
            @RequestBody PalabraProhibidaDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        PalabraProhibidaDTO creada = filtroContenidoService.crearPalabra(dto, userDetails.getUsername());
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar palabra o frase prohibida")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Regla actualizada"),
            @ApiResponse(responseCode = "404", description = "Regla no encontrada")
    })
    @PutMapping("/palabras/{id}")
    public ResponseEntity<PalabraProhibidaDTO> actualizarPalabra(
            @PathVariable String id,
            @RequestBody PalabraProhibidaDTO dto) {
        return ResponseEntity.ok(filtroContenidoService.actualizarPalabra(id, dto));
    }

    @Operation(summary = "Eliminar palabra o frase prohibida")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Regla eliminada"),
            @ApiResponse(responseCode = "404", description = "Regla no encontrada")
    })
    @DeleteMapping("/palabras/{id}")
    public ResponseEntity<Void> eliminarPalabra(@PathVariable String id) {
        filtroContenidoService.eliminarPalabra(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Activar / desactivar palabra o frase prohibida")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado cambiado"),
            @ApiResponse(responseCode = "404", description = "Regla no encontrada")
    })
    @PatchMapping("/palabras/{id}/toggle")
    public ResponseEntity<PalabraProhibidaDTO> togglePalabra(@PathVariable String id) {
        return ResponseEntity.ok(filtroContenidoService.togglePalabra(id));
    }

    // ---- Historial de moderación ----

    @Operation(summary = "Historial de moderación", description = "Registros de acciones del filtro automático con filtros opcionales y paginación")
    @GetMapping("/historial")
    public ResponseEntity<Page<RegistroModeracionDTO>> listarHistorial(
            @Parameter(description = "Filtrar por acción") @RequestParam(required = false) AccionModeracion accion,
            @Parameter(description = "Filtrar por nombre de usuario (contiene)") @RequestParam(required = false) String usuario,
            @Parameter(description = "Filtrar por tipo de contenido") @RequestParam(required = false) TipoContenidoModerado tipoContenido,
            @Parameter(description = "Desde (ISO date-time)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @Parameter(description = "Hasta (ISO date-time)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @Parameter(description = "Número de página (inicia en 0)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de registros por página", example = "10") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha"));
        return ResponseEntity.ok(filtroContenidoService.listarHistorial(accion, usuario, tipoContenido, desde, hasta,
                pageable));
    }

    // ---- Apelaciones de bans ----

    @Operation(summary = "Apelaciones pendientes", description = "Bans indefinidos con apelación pendiente de revisión")
    @GetMapping("/apelaciones")
    public ResponseEntity<List<RegistroModeracionDTO>> listarApelacionesPendientes() {
        return ResponseEntity.ok(filtroContenidoService.listarApelacionesPendientes());
    }

    @Operation(summary = "Resolver apelación", description = "Aprueba (reactiva la cuenta) o rechaza (mantiene el ban) una apelación")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Apelación resuelta"),
            @ApiResponse(responseCode = "400", description = "El registro no tiene apelación pendiente"),
            @ApiResponse(responseCode = "404", description = "Registro no encontrado")
    })
    @PostMapping("/apelaciones/{id}/resolver")
    public ResponseEntity<RegistroModeracionDTO> resolverApelacion(
            @PathVariable String id,
            @Valid @RequestBody ResolverApelacionRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(filtroContenidoService.resolverApelacion(id, request, userDetails.getUsername()));
    }
}
