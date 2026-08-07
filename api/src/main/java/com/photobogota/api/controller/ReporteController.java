package com.photobogota.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.photobogota.api.dto.CambiarEstadoRequestDTO;
import com.photobogota.api.dto.CrearReporteRequestDTO;
import com.photobogota.api.dto.EscalarReporteRequestDTO;
import com.photobogota.api.dto.ReporteResponseDTO;
import com.photobogota.api.dto.ValidarReporteRequestDTO;
import com.photobogota.api.model.CategoriaReporte;
import com.photobogota.api.model.EstadoReporte;
import com.photobogota.api.model.Gravedad;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.TipoObjetivoReporte;
import com.photobogota.api.service.IReporteService;
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
@RequestMapping(ApiConstants.V1 + "/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Sistema de reportes de la comunidad. Etapa 1: creación y asignación automática (MOD/ADMIN/SOCIO). "
        + "Etapa 2: dashboard, cambio de estado y escalamiento entre SOCIO, MOD y ADMIN, con validación de moderación.")
public class ReporteController {

    private final IReporteService reporteService;

    @Operation(summary = "Crear un reporte", description = "Crea un reporte, genera un número de ticket y lo asigna automáticamente. "
            + "Si el reporte es sobre el local de un SOCIO, se asigna primero al SOCIO. "
            + "Si es sobre cualquier otro spot, se asigna a MOD. El resto se asigna por categoría (error técnico a ADMIN).", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reporte creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReporteResponseDTO> crearReporte(
            @Valid @RequestBody CrearReporteRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String usuario = userDetails.getUsername();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reporteService.crearReporte(request, usuario));
    }

    @Operation(summary = "Ver mis reportes", description = "Lista los reportes creados por el usuario autenticado.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/mios")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReporteResponseDTO>> listarMisReportes(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reporteService.listarMisReportes(userDetails.getUsername()));
    }

    @Operation(summary = "Obtener detalle de un reporte", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle del reporte"),
            @ApiResponse(responseCode = "404", description = "Reporte no encontrado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReporteResponseDTO> obtenerReporte(@PathVariable String id) {
        return ResponseEntity.ok(reporteService.obtenerPorId(id));
    }

    @Operation(summary = "Listar reportes asignados a moderadores", description = "Cola de reportes de categoría ofensivo, spam, información incorrecta y cualquier reporte sobre un spot (excepto locales de socio).", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/asignados/moderador")
    @PreAuthorize("hasAnyRole('MOD', 'ADMIN')")
    public ResponseEntity<List<ReporteResponseDTO>> listarAsignadosAModerador() {
        return ResponseEntity.ok(reporteService.listarPorRolAsignado(Rol.MOD, null));
    }

    @Operation(summary = "Listar reportes asignados a administradores", description = "Cola de reportes de categoría error técnico (sin spot asociado) y los escalados por moderación.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/asignados/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReporteResponseDTO>> listarAsignadosAAdmin() {
        return ResponseEntity.ok(reporteService.listarPorRolAsignado(Rol.ADMIN, null));
    }

    @Operation(summary = "Listar reportes asignados a mis locales (SOCIO)", description = "Cola de reportes sobre los locales del socio autenticado.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/asignados/socio")
    @PreAuthorize("hasRole('SOCIO')")
    public ResponseEntity<List<ReporteResponseDTO>> listarAsignadosASocio(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reporteService.listarPorRolAsignado(Rol.SOCIO, userDetails.getUsername()));
    }

    @Operation(summary = "Dashboard de reportes (Etapa 2)", description = "Lista filtrable y priorizada automáticamente. "
            + "ADMIN ve lo asignado a administración (categoría error técnico sin spot + reportes escalados). "
            + "MOD ve solo lo asignado a moderación. SOCIO ve solo lo asignado a SOCIO y de sus propios locales. "
            + "Todos los filtros son opcionales y combinables.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('MOD', 'ADMIN', 'SOCIO')")
    public ResponseEntity<List<ReporteResponseDTO>> obtenerDashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Filtra por estado: NUEVO, EN_REVISION, PENDIENTE_VALIDACION, RESUELTO, RECHAZADO") @RequestParam(required = false) EstadoReporte estado,
            @Parameter(description = "Filtra por gravedad: BAJA, MEDIA, ALTA, CRITICA") @RequestParam(required = false) Gravedad gravedad,
            @Parameter(description = "Filtra por categoria/tipo de reporte") @RequestParam(required = false) CategoriaReporte categoria,
            @Parameter(description = "Filtra por que se reporto: SPOT o RESENA") @RequestParam(required = false) TipoObjetivoReporte tipoObjetivo,
            @Parameter(description = "Filtra solo reportes escalados (true) o no escalados (false)") @RequestParam(required = false) Boolean escalado,
            @Parameter(description = "Orden: recientes (default), antiguos, prioridad") @RequestParam(required = false) String orden) {

        Rol rolUsuario = obtenerRol(userDetails);
        return ResponseEntity.ok(reporteService.obtenerDashboard(
                rolUsuario, userDetails.getUsername(), estado, gravedad, categoria, tipoObjetivo, escalado, orden));
    }

    @Operation(summary = "Cambiar el estado de un reporte", description = "Un SOCIO o un MOD solo pueden cambiar el estado de reportes asignados a su propia cola. Un ADMIN puede cambiar cualquier reporte. "
            + "Si un SOCIO o un ADMIN marcan el reporte como RESUELTO, queda pendiente de validación de un MOD antes de notificar al miembro afectado.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "403", description = "El reporte no pertenece a la cola de quien intenta cambiarlo"),
            @ApiResponse(responseCode = "404", description = "Reporte no encontrado")
    })
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('MOD', 'ADMIN', 'SOCIO')")
    public ResponseEntity<ReporteResponseDTO> cambiarEstado(
            @PathVariable String id,
            @Valid @RequestBody CambiarEstadoRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Rol rolUsuario = obtenerRol(userDetails);
        return ResponseEntity.ok(
                reporteService.cambiarEstado(id, request, userDetails.getUsername(), rolUsuario));
    }

    @Operation(summary = "Escalar un reporte al siguiente nivel", description = "Un SOCIO escala su reporte a un moderador. Un moderador escala su reporte a un administrador. "
            + "Un administrador no puede escalar más (es el último nivel).", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte escalado"),
            @ApiResponse(responseCode = "400", description = "El reporte ya está en el nivel más alto de escalamiento"),
            @ApiResponse(responseCode = "403", description = "No tienes permiso para escalar este reporte"),
            @ApiResponse(responseCode = "404", description = "Reporte no encontrado")
    })
    @PatchMapping("/{id}/escalar")
    @PreAuthorize("hasAnyRole('SOCIO', 'MOD')")
    public ResponseEntity<ReporteResponseDTO> escalarReporte(
            @PathVariable String id,
            @Valid @RequestBody EscalarReporteRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Rol rolUsuario = obtenerRol(userDetails);
        return ResponseEntity.ok(
                reporteService.escalarReporte(id, request, userDetails.getUsername(), rolUsuario));
    }

    @Operation(summary = "Listar reportes pendientes de validación", description = "Reportes que un SOCIO o un ADMIN marcaron como solucionados y esperan que un MOD apruebe o rechace la solución.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/pendientes-validacion")
    @PreAuthorize("hasRole('MOD')")
    public ResponseEntity<List<ReporteResponseDTO>> listarPendientesValidacion() {
        return ResponseEntity.ok(reporteService.listarPendientesValidacion());
    }

    @Operation(summary = "Validar (aprobar o rechazar) la solución de un reporte", description = "Solo un MOD puede validar. Si aprueba, el reporte pasa a RESUELTO y se notifica automáticamente al miembro afectado. "
            + "Si rechaza, el reporte vuelve a EN_REVISION para que quien lo resolvió lo revise de nuevo.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validación registrada"),
            @ApiResponse(responseCode = "400", description = "El reporte no está pendiente de validación"),
            @ApiResponse(responseCode = "404", description = "Reporte no encontrado")
    })
    @PatchMapping("/{id}/validar")
    @PreAuthorize("hasRole('MOD')")
    public ResponseEntity<ReporteResponseDTO> validarReporte(
            @PathVariable String id,
            @Valid @RequestBody ValidarReporteRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                reporteService.validarReporte(id, request, userDetails.getUsername()));
    }

    // Los endpoints ya están protegidos con @PreAuthorize a MOD/ADMIN/SOCIO,
    // así que basta con distinguir cuál de los tres es para aplicar las
    // reglas de negocio (ownership, visibilidad del dashboard, quién puede
    // escalar a quién).
    private Rol obtenerRol(UserDetails userDetails) {
        List<String> authorities = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList();
        if (authorities.contains("ROLE_ADMIN")) {
            return Rol.ADMIN;
        }
        if (authorities.contains("ROLE_MOD")) {
            return Rol.MOD;
        }
        return Rol.SOCIO;
    }
}
