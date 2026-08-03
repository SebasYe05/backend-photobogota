package com.photobogota.api.controller;

import com.photobogota.api.dto.AspiranteResponseDTO;
import com.photobogota.api.dto.ComentarioInternoRequestDTO;
import com.photobogota.api.dto.DecisionAspiranteDTO;
import com.photobogota.api.dto.EstadisticasAspiranteDTO;
import com.photobogota.api.dto.ReenvioDocumentosDTO;
import com.photobogota.api.dto.SolicitudAspiranteDTO;
import com.photobogota.api.model.EstadoAspirante;
import com.photobogota.api.service.IAspiranteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/aspirantes")
@RequiredArgsConstructor
@Tag(name = "Aspirantes", description = "Gestión de solicitudes de aspirantes a socios")
public class AspiranteController {

    private final IAspiranteService aspiranteService;

    @Operation(summary = "Enviar solicitud de ingreso", description = "Crea una nueva solicitud para unirse a PhotoBogotá como fotógrafo. No requiere cuenta previa.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitud creada, pendiente de revisión"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o email ya tiene solicitud activa")
    })
    @PostMapping
    public ResponseEntity<AspiranteResponseDTO> crearSolicitud(
            @Valid @RequestBody SolicitudAspiranteDTO request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(aspiranteService.crearSolicitud(request));
    }

    @Operation(summary = "Obtener solicitud por ID", description = "Requiere rol MOD o ADMIN.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud encontrada"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MOD', 'ADMIN')")
    public ResponseEntity<AspiranteResponseDTO> obtenerPorId(
            @Parameter(description = "ID de la solicitud", required = true) @PathVariable String id) {
        return ResponseEntity.ok(aspiranteService.obtenerPorId(id));
    }

    @Operation(summary = "Obtener solicitud por email", description = "Requiere rol MOD o ADMIN.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud encontrada"),
            @ApiResponse(responseCode = "404", description = "No existe solicitud con ese email")
    })
    @GetMapping("/email/{email}")
    @PreAuthorize("hasAnyRole('MOD', 'ADMIN')")
    public ResponseEntity<AspiranteResponseDTO> obtenerPorEmail(
            @Parameter(description = "Email del aspirante", example = "juan.romero@example.com") @PathVariable String email) {
        return ResponseEntity.ok(aspiranteService.obtenerPorEmail(email));
    }

    @Operation(summary = "Obtener solicitud por código de seguimiento", description = "Pública: la usa el aspirante (sin cuenta) para consultar el estado de su propia solicitud.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud encontrada"),
            @ApiResponse(responseCode = "404", description = "Código no encontrado")
    })
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<AspiranteResponseDTO> obtenerPorCodigo(
            @Parameter(description = "Código único de seguimiento de la solicitud") @PathVariable String codigo) {
        return ResponseEntity.ok(aspiranteService.obtenerPorCodigo(codigo));
    }

    @Operation(summary = "Listar todas las solicitudes", description = "Retorna todas las solicitudes de aspirantes sin filtrar por estado. Requiere rol MOD o ADMIN.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Lista de solicitudes")
    @GetMapping
    @PreAuthorize("hasAnyRole('MOD', 'ADMIN')")
    public ResponseEntity<List<AspiranteResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(aspiranteService.obtenerTodos());
    }

    @Operation(summary = "Filtrar solicitudes por estado", description = "Retorna solicitudes según su estado. Requiere rol MOD o ADMIN.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada por estado"),
            @ApiResponse(responseCode = "400", description = "Estado no válido")
    })
    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasAnyRole('MOD', 'ADMIN')")
    public ResponseEntity<List<AspiranteResponseDTO>> obtenerPorEstado(
            @Parameter(description = "Estado de la solicitud", example = "PENDIENTE") @PathVariable EstadoAspirante estado) {
        return ResponseEntity.ok(aspiranteService.obtenerPorEstado(estado));
    }

    @Operation(summary = "Estadísticas de solicitudes", description = "Contadores de solicitudes por estado, para el panel del moderador. Requiere rol MOD o ADMIN.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Estadísticas calculadas")
    @GetMapping("/estadisticas")
    @PreAuthorize("hasAnyRole('MOD', 'ADMIN')")
    public ResponseEntity<EstadisticasAspiranteDTO> obtenerEstadisticas() {
        return ResponseEntity.ok(aspiranteService.obtenerEstadisticas());
    }

    @Operation(summary = "Aprobar solicitud", description = "Aprueba un aspirante y lo deja en espera de envío de credenciales. Requiere rol MOD o ADMIN.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aspirante aprobado, en espera de envío de credenciales"),
            @ApiResponse(responseCode = "400", description = "La solicitud ya fue procesada"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos suficientes")
    })
    @PutMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('MOD', 'ADMIN')")
    public ResponseEntity<AspiranteResponseDTO> aprobarAspirante(
            @Parameter(description = "ID de la solicitud a aprobar", required = true) @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(aspiranteService.aprobarAspirante(id, userDetails.getUsername()));
    }

    @Operation(summary = "Rechazar solicitud", description = "Rechaza la solicitud de un aspirante con una justificación obligatoria. Requiere rol MOD o ADMIN.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud rechazada"),
            @ApiResponse(responseCode = "400", description = "Falta la justificación o la solicitud ya fue procesada"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos suficientes")
    })
    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('MOD', 'ADMIN')")
    public ResponseEntity<AspiranteResponseDTO> rechazarAspirante(
            @Parameter(description = "ID de la solicitud a rechazar", required = true) @PathVariable String id,
            @Valid @RequestBody DecisionAspiranteDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                aspiranteService.rechazarAspirante(id, request.getMotivo(), userDetails.getUsername()));
    }

    @Operation(summary = "Solicitar correcciones", description = "Devuelve la solicitud al aspirante para que corrija datos o vuelva a subir documentos. Requiere rol MOD o ADMIN.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud devuelta para corrección"),
            @ApiResponse(responseCode = "400", description = "Falta la justificación o la solicitud ya fue procesada"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos suficientes")
    })
    @PutMapping("/{id}/solicitar-correccion")
    @PreAuthorize("hasAnyRole('MOD', 'ADMIN')")
    public ResponseEntity<AspiranteResponseDTO> solicitarCorreccion(
            @Parameter(description = "ID de la solicitud", required = true) @PathVariable String id,
            @Valid @RequestBody DecisionAspiranteDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                aspiranteService.solicitarCorreccion(id, request.getMotivo(), userDetails.getUsername()));
    }

    @Operation(summary = "Reenviar documentos tras una corrección", description = "Pública: el aspirante (sin cuenta) reenvía su documentación usando su código de seguimiento. Solo funciona si la solicitud está en corrección.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documentos reenviados, la solicitud vuelve a estar pendiente"),
            @ApiResponse(responseCode = "400", description = "La solicitud no está en estado de corrección"),
            @ApiResponse(responseCode = "404", description = "Código no encontrado")
    })
    @PutMapping("/codigo/{codigo}/reenviar")
    public ResponseEntity<AspiranteResponseDTO> reenviarDocumentos(
            @Parameter(description = "Código de seguimiento de la solicitud", required = true) @PathVariable String codigo,
            @Valid @RequestBody ReenvioDocumentosDTO request) {
        return ResponseEntity.ok(aspiranteService.reenviarDocumentos(codigo, request));
    }

    @Operation(summary = "Agregar comentario interno", description = "Comentario visible solo para moderadores/administradores, para coordinar la revisión. Requiere rol MOD o ADMIN.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comentario agregado"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    })
    @PostMapping("/{id}/comentarios")
    @PreAuthorize("hasAnyRole('MOD', 'ADMIN')")
    public ResponseEntity<AspiranteResponseDTO> agregarComentario(
            @Parameter(description = "ID de la solicitud", required = true) @PathVariable String id,
            @Valid @RequestBody ComentarioInternoRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                aspiranteService.agregarComentarioInterno(id, request.getTexto(), userDetails.getUsername()));
    }

    @Operation(summary = "Enviar credenciales y crear cuenta de socio", description = "Crea automáticamente la cuenta de usuario con rol SOCIO para el aspirante aprobado y le envía sus credenciales por correo. Requiere rol MOD o ADMIN.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta creada y credenciales enviadas"),
            @ApiResponse(responseCode = "400", description = "La solicitud no está en espera de envío de credenciales o ya se enviaron"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos suficientes")
    })
    @PutMapping("/{id}/enviar-credenciales")
    @PreAuthorize("hasAnyRole('MOD', 'ADMIN')")
    public ResponseEntity<AspiranteResponseDTO> enviarCredenciales(
            @Parameter(description = "ID de la solicitud", required = true) @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(aspiranteService.enviarCredenciales(id, userDetails.getUsername()));
    }

    @Operation(summary = "Actualizar estado manualmente", description = "Permite cambiar el estado de una solicitud a cualquier valor, sin pasar por las reglas de negocio. Requiere rol ADMIN.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    })
    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AspiranteResponseDTO> actualizarEstado(
            @Parameter(description = "ID de la solicitud", required = true) @PathVariable String id,
            @Parameter(description = "Nuevo estado", example = "APROBADO") @RequestParam EstadoAspirante estado) {
        return ResponseEntity.ok(aspiranteService.actualizarEstado(id, estado));
    }
}
