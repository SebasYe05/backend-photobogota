package com.photobogota.api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.photobogota.api.dto.CrearPromocionRequestDTO;
import com.photobogota.api.dto.PromocionResponseDTO;
import com.photobogota.api.exception.AccessForbiddenException;
import com.photobogota.api.exception.OperacionInvalidaException;
import com.photobogota.api.exception.ResourceNotFoundException;
import com.photobogota.api.mapper.PromocionMapper;
import com.photobogota.api.model.Promocion;
import com.photobogota.api.model.Spot;
import com.photobogota.api.model.TipoContenidoModerado;
import com.photobogota.api.repository.PromocionRepository;
import com.photobogota.api.repository.SpotRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromocionService {

    private final PromocionRepository promocionRepository;
    private final SpotRepository spotRepository;
    private final PromocionMapper promocionMapper;
    private final IFiltroContenidoService filtroContenidoService;

    public PromocionResponseDTO crearPromocion(CrearPromocionRequestDTO request, String usuario) {
        filtroContenidoService.verificarPermisoPublicar(usuario);
        filtroContenidoService.validarContenido(usuario, TipoContenidoModerado.PROMOCION_TITULO, request.getTitulo());
        filtroContenidoService.validarContenido(usuario, TipoContenidoModerado.PROMOCION_DESCRIPCION,
                request.getDescripcion());

        Spot spot = spotRepository.findById(request.getSpotId())
                .orElseThrow(() -> new ResourceNotFoundException("Local no encontrado con id: " + request.getSpotId()));

        if (!"SOCIO".equalsIgnoreCase(spot.getCreadorRol())) {
            throw new OperacionInvalidaException("Solo se pueden crear promociones para locales de socios");
        }
        if (!usuario.equalsIgnoreCase(spot.getCreadorUsername())) {
            throw new AccessForbiddenException("Solo el socio dueño del local puede crear sus promociones");
        }

        LocalDateTime inicio = LocalDate.parse(request.getFechaInicio()).atStartOfDay();
        LocalDateTime fin = LocalDate.parse(request.getFechaFin()).atTime(LocalTime.MAX);
        if (fin.isBefore(inicio)) {
            throw new OperacionInvalidaException("La fecha de fin debe ser posterior a la fecha de inicio");
        }

        Promocion promocion = Promocion.builder()
                .spotId(spot.getId())
                .socioUsername(usuario)
                .nombreSpot(spot.getNombre())
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .tipo(request.getTipo())
                .descuento(request.getDescuento())
                .codigo(request.getCodigo())
                .imagenes(request.getImagenes() != null ? request.getImagenes() : List.of())
                .fechaInicio(inicio)
                .fechaFin(fin)
                .activo(true)
                .usos(0)
                .usosMaximos(request.getUsosMaximos())
                .fechaCreacion(LocalDateTime.now())
                .build();

        Promocion guardada = promocionRepository.save(promocion);
        return promocionMapper.toResponse(guardada);
    }

    public PromocionResponseDTO actualizarPromocion(String id, CrearPromocionRequestDTO request, String usuario) {
        Promocion promocion = obtenerPromocionPropia(id, usuario);

        filtroContenidoService.validarContenido(usuario, TipoContenidoModerado.PROMOCION_TITULO, request.getTitulo());
        filtroContenidoService.validarContenido(usuario, TipoContenidoModerado.PROMOCION_DESCRIPCION,
                request.getDescripcion());

        promocion.setTitulo(request.getTitulo());
        promocion.setDescripcion(request.getDescripcion());
        promocion.setTipo(request.getTipo());
        promocion.setDescuento(request.getDescuento());
        promocion.setCodigo(request.getCodigo());
        if (request.getImagenes() != null) {
            promocion.setImagenes(request.getImagenes());
        }
        LocalDateTime inicio = LocalDate.parse(request.getFechaInicio()).atStartOfDay();
        LocalDateTime fin = LocalDate.parse(request.getFechaFin()).atTime(LocalTime.MAX);
        if (fin.isBefore(inicio)) {
            throw new OperacionInvalidaException("La fecha de fin debe ser posterior a la fecha de inicio");
        }
        promocion.setFechaInicio(inicio);
        promocion.setFechaFin(fin);
        promocion.setUsosMaximos(request.getUsosMaximos());

        return promocionMapper.toResponse(promocionRepository.save(promocion));
    }

    public List<PromocionResponseDTO> listarMias(String usuario) {
        return promocionRepository.findBySocioUsername(usuario).stream()
                .map(promocionMapper::toResponse)
                .toList();
    }

    public PromocionResponseDTO obtenerPorId(String id) {
        Promocion promocion = promocionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción no encontrada con id: " + id));
        return promocionMapper.toResponse(promocion);
    }

    public List<PromocionResponseDTO> listarDeSpot(String spotId) {
        return promocionRepository.findBySpotId(spotId).stream()
                .map(promocionMapper::toResponse)
                .toList();
    }

    /**
     * Promoción actualmente vigente y activa de un local (la que ve el público
     * al entrar a la página del local). Devuelve la más reciente si hay varias.
     */
    public PromocionResponseDTO obtenerActivaDeSpot(String spotId) {
        return promocionRepository.findBySpotId(spotId).stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .filter(this::estaVigente)
                .max((a, b) -> a.getFechaInicio().compareTo(b.getFechaInicio()))
                .map(promocionMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Este local no tiene una promoción activa en este momento"));
    }

    /**
     * Promociones visibles al público: activas y dentro del rango de fechas.
     * Usado por el mapa para saber qué locales tienen promoción vigente.
     */
    public List<PromocionResponseDTO> listarActivas() {
        return promocionRepository.findByActivoTrue().stream()
                .filter(this::estaVigente)
                .map(promocionMapper::toResponse)
                .toList();
    }

    public Set<String> obtenerSpotIdsConPromocionActiva() {
        return promocionRepository.findByActivoTrue().stream()
                .filter(this::estaVigente)
                .map(Promocion::getSpotId)
                .collect(Collectors.toSet());
    }

    public boolean tienePromocionActiva(String spotId) {
        return promocionRepository.findBySpotId(spotId).stream()
                .anyMatch(p -> Boolean.TRUE.equals(p.getActivo()) && estaVigente(p));
    }

    public PromocionResponseDTO desactivarPromocion(String id, String usuario) {
        Promocion promocion = obtenerPromocionPropia(id, usuario);
        promocion.setActivo(Boolean.FALSE.equals(promocion.getActivo()));
        return promocionMapper.toResponse(promocionRepository.save(promocion));
    }

    public PromocionResponseDTO duplicarPromocion(String id, String usuario) {
        Promocion origen = obtenerPromocionPropia(id, usuario);

        Promocion copia = Promocion.builder()
                .spotId(origen.getSpotId())
                .socioUsername(usuario)
                .nombreSpot(origen.getNombreSpot())
                .titulo(origen.getTitulo())
                .descripcion(origen.getDescripcion())
                .tipo(origen.getTipo())
                .descuento(origen.getDescuento())
                .codigo(origen.getCodigo())
                .imagenes(origen.getImagenes())
                .fechaInicio(LocalDate.now().atStartOfDay())
                .fechaFin(LocalDate.now().plusDays(30).atTime(LocalTime.MAX))
                .activo(false)
                .usos(0)
                .usosMaximos(origen.getUsosMaximos())
                .fechaCreacion(LocalDateTime.now())
                .build();

        return promocionMapper.toResponse(promocionRepository.save(copia));
    }

    public void eliminarPromocion(String id, String usuario) {
        Promocion promocion = obtenerPromocionPropia(id, usuario);
        promocionRepository.delete(promocion);
    }

    private Promocion obtenerPromocionPropia(String id, String usuario) {
        Promocion promocion = promocionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción no encontrada con id: " + id));
        if (!usuario.equalsIgnoreCase(promocion.getSocioUsername())) {
            throw new AccessForbiddenException("Solo el socio dueño puede gestionar esta promoción");
        }
        return promocion;
    }

    private boolean estaVigente(Promocion promocion) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicio = promocion.getFechaInicio();
        LocalDateTime fin = promocion.getFechaFin();
        if (inicio != null && inicio.isAfter(ahora)) {
            return false;
        }
        if (fin != null && fin.isBefore(ahora)) {
            return false;
        }
        return true;
    }
}