package com.photobogota.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.photobogota.api.dto.CrearReporteRequestDTO;
import com.photobogota.api.dto.ReporteResponseDTO;
import com.photobogota.api.exception.AccessForbiddenException;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.model.Calificacion;
import com.photobogota.api.model.CategoriaReporte;
import com.photobogota.api.model.EstadoReporte;
import com.photobogota.api.model.Reporte;
import com.photobogota.api.model.Rol;
import com.photobogota.api.model.Spot;
import com.photobogota.api.model.TipoObjetivoReporte;
import com.photobogota.api.repository.CalificacionRepository;
import com.photobogota.api.repository.ReporteRepository;
import com.photobogota.api.repository.SpotRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements IReporteService {

    private final ReporteRepository reporteRepository;
    private final SpotRepository spotRepository;
    private final CalificacionRepository calificacionRepository;

    @Override
    public ReporteResponseDTO crearReporte(CrearReporteRequestDTO request, String usuario) {

        Spot spot = null;
        Calificacion calificacionReportada = null;

        // Si viene spotId, lo resolvemos una sola vez para sacar nombre y si es
        // un local de socio. Las reseñas NO viven embebidas en Spot: se guardan
        // en la colección aparte "calificaciones" (ver CalificacionRepository),
        // así que resenaId se busca ahí, no dentro de spot.getResenas().
        if (request.getSpotId() != null && !request.getSpotId().isBlank()) {
            spot = spotRepository.findById(request.getSpotId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Spot no encontrado con id: " + request.getSpotId()));

            if (request.getResenaId() != null && !request.getResenaId().isBlank()) {
                calificacionReportada = calificacionRepository.findById(request.getResenaId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Reseña no encontrada con id: " + request.getResenaId()));

                if (!request.getSpotId().equals(calificacionReportada.getSpotId())) {
                    throw new ResourceNotFoundException(
                            "La reseña " + request.getResenaId() + " no pertenece al spot " + request.getSpotId());
                }

                // Un miembro no puede reportar su propia reseña.
                if (usuario != null && usuario.equals(calificacionReportada.getUsuario())) {
                    throw new AccessForbiddenException("No puedes reportar tu propia reseña");
                }
            }
        } else if (request.getResenaId() != null && !request.getResenaId().isBlank()) {
            // No debería pasar desde el front (siempre se manda spotId junto con
            // resenaId), pero lo validamos por si llega una petición directa a la API.
            throw new ResourceNotFoundException(
                    "Debes indicar el spotId al que pertenece la reseña reportada");
        }

        TipoObjetivoReporte tipoObjetivo = calificacionReportada != null
                ? TipoObjetivoReporte.RESENA
                : TipoObjetivoReporte.SPOT;

        Reporte reporte = Reporte.builder()
                .numeroTicket(generarNumeroTicketUnico())
                .categoria(request.getCategoria())
                .descripcion(request.getDescripcion())
                .evidencias(request.getEvidencias() != null ? request.getEvidencias() : List.of())
                .reportadoPor(usuario)
                .tipoObjetivo(tipoObjetivo)
                .spotId(request.getSpotId())
                .nombreSpot(spot != null ? spot.getNombre() : null)
                .esLocalDeSocio(spot != null ? "SOCIO".equals(spot.getCreadorRol()) : null)
                .resenaId(calificacionReportada != null ? calificacionReportada.getId() : null)
                .autorResenaReportada(calificacionReportada != null ? calificacionReportada.getUsuario() : null)
                .asignadoA(asignarResponsable(request.getCategoria()))
                .estado(EstadoReporte.NUEVO)
                .fechaCreacion(LocalDateTime.now())
                .build();

        Reporte guardado = reporteRepository.save(reporte);
        return mapearADTO(guardado);
    }

    @Override
    public ReporteResponseDTO obtenerPorId(String id) {
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado con id: " + id));
        return mapearADTO(reporte);
    }

    @Override
    public List<ReporteResponseDTO> listarMisReportes(String usuario) {
        return reporteRepository.findByReportadoPor(usuario).stream()
                .map(this::mapearADTO)
                .toList();
    }

    @Override
    public List<ReporteResponseDTO> listarPorRolAsignado(Rol rol) {
        return reporteRepository.findByAsignadoA(rol).stream()
                .map(this::mapearADTO)
                .toList();
    }

    // Asignación automática (Etapa 1, punto 3):
    // - Error técnico -> ADMIN
    // - Contenido ofensivo, spam, información incorrecta y problema con spot -> MOD
    private Rol asignarResponsable(CategoriaReporte categoria) {
        return switch (categoria) {
            case ERROR_TECNICO -> Rol.ADMIN;
            case CONTENIDO_OFENSIVO, SPAM, INFORMACION_INCORRECTA, PROBLEMA_SPOT -> Rol.MOD;
        };
    }

    // Genera un ticket tipo "REP-482913" verificando unicidad contra la base de datos
    private String generarNumeroTicketUnico() {
        String numeroTicket;
        do {
            String numeros = String.format("%06d", new Random().nextInt(999999));
            numeroTicket = "REP-" + numeros;
        } while (reporteRepository.existsByNumeroTicket(numeroTicket));
        return numeroTicket;
    }

    private ReporteResponseDTO mapearADTO(Reporte reporte) {
        return ReporteResponseDTO.builder()
                .id(reporte.getId())
                .numeroTicket(reporte.getNumeroTicket())
                .categoria(reporte.getCategoria())
                .descripcion(reporte.getDescripcion())
                .evidencias(reporte.getEvidencias())
                .reportadoPor(reporte.getReportadoPor())
                .tipoObjetivo(reporte.getTipoObjetivo())
                .spotId(reporte.getSpotId())
                .nombreSpot(reporte.getNombreSpot())
                .esLocalDeSocio(reporte.getEsLocalDeSocio())
                .resenaId(reporte.getResenaId())
                .autorResenaReportada(reporte.getAutorResenaReportada())
                .asignadoA(reporte.getAsignadoA())
                .estado(reporte.getEstado())
                .fechaCreacion(reporte.getFechaCreacion())
                .fechaActualizacion(reporte.getFechaActualizacion())
                .build();
    }
}
