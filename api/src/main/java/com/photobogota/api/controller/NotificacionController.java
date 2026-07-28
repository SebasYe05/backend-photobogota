package com.photobogota.api.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.photobogota.api.dto.ContadorNotificacionesDTO;
import com.photobogota.api.dto.EnviarNotificacionRequestDTO;
import com.photobogota.api.dto.NotificacionResponseDTO;
import com.photobogota.api.dto.PreferenciasNotificacionDTO;
import com.photobogota.api.model.Notificacion;
import com.photobogota.api.service.INotificacionService;
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
@RequestMapping(ApiConstants.V1 + "/notificaciones")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notificaciones", description = "Notificaciones personalizadas de actividad relevante (HU #40)")
public class NotificacionController {

    private final INotificacionService notificacionService;

    @Operation(summary = "Listar mis notificaciones", description = "Bandeja paginada de notificaciones del usuario autenticado. El front/mobile puede pollear este endpoint periódicamente.")
    @ApiResponse(responseCode = "200", description = "Página de notificaciones")
    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<NotificacionResponseDTO>> listarMisNotificaciones(
            @Parameter(description = "Si es true, solo trae las no leídas") @RequestParam(required = false) Boolean soloNoLeidas,
            @Parameter(description = "Número de página (0-indexado)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, Notificacion.FIELD_FECHA_CREACION));
        return ResponseEntity.ok(
                notificacionService.listarMisNotificaciones(userDetails.getUsername(), pageable, soloNoLeidas));
    }

    @Operation(summary = "Contar notificaciones no leídas", description = "Útil para mostrar el contador (badge) en el front/mobile.")
    @ApiResponse(responseCode = "200", description = "Cantidad de notificaciones no leídas")
    @GetMapping("/no-leidas/contador")
    public ResponseEntity<ContadorNotificacionesDTO> contarNoLeidas(@AuthenticationPrincipal UserDetails userDetails) {
        long total = notificacionService.contarNoLeidas(userDetails.getUsername());
        return ResponseEntity.ok(new ContadorNotificacionesDTO(total));
    }

    @Operation(summary = "Marcar una notificación como leída")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Marcada como leída"),
            @ApiResponse(responseCode = "404", description = "Notificación no encontrada")
    })
    @PatchMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        notificacionService.marcarLeida(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Marcar todas mis notificaciones como leídas")
    @ApiResponse(responseCode = "204", description = "Todas marcadas como leídas")
    @PatchMapping("/leer-todas")
    public ResponseEntity<Void> marcarTodasLeidas(@AuthenticationPrincipal UserDetails userDetails) {
        notificacionService.marcarTodasLeidas(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Eliminar una notificación")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notificación eliminada"),
            @ApiResponse(responseCode = "404", description = "Notificación no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarNotificacion(@PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        notificacionService.eliminarNotificacion(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obtener mis preferencias de notificaciones")
    @ApiResponse(responseCode = "200", description = "Preferencias actuales (o valores por defecto si nunca las ha configurado)")
    @GetMapping("/preferencias")
    public ResponseEntity<PreferenciasNotificacionDTO> obtenerPreferencias(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(notificacionService.obtenerPreferencias(userDetails.getUsername()));
    }

    @Operation(summary = "Actualizar mis preferencias de notificaciones", description = "Actualización parcial: solo se modifican los campos enviados (activar/desactivar, canal, tipos silenciados, zonas/categorías de interés).")
    @ApiResponse(responseCode = "200", description = "Preferencias actualizadas")
    @PutMapping("/preferencias")
    public ResponseEntity<PreferenciasNotificacionDTO> actualizarPreferencias(
            @RequestBody PreferenciasNotificacionDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(notificacionService.actualizarPreferencias(userDetails.getUsername(), dto));
    }

    @Operation(summary = "Enviar notificación manual (anuncio)", description = "Permite a un Admin o Moderador enviar un anuncio a todos los usuarios, a uno o varios roles, o a usuarios específicos.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Notificación enviada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente")
    })
    @PostMapping("/enviar")
    @PreAuthorize("hasAnyRole('ADMIN', 'MOD')")
    public ResponseEntity<Void> enviarNotificacion(@Valid @RequestBody EnviarNotificacionRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String rol = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .map(a -> a.substring(5))
                .orElse(null);

        notificacionService.enviarNotificacionManual(request, userDetails.getUsername(), rol);
        return ResponseEntity.accepted().build();
    }

    // Validaciones de alcance (ej. "POR_ROL" sin roles indicados) devuelven 400 en
    // vez de 500.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleAlcanceInvalido(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
